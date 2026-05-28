'use server'

import { createClient } from '@/utils/supabase/server';
import { cookies } from 'next/headers'
import { z } from 'zod'

const inviteSchema = z.object({
    email: z.string().email(),
    role: z.enum(['admin', 'manager', 'member']),
    projectIds: z.array(z.string()),
    permissions: z.record(z.any()).optional(),
})

export type InviteEmployeeState = {
    success?: boolean
    error?: string
    fieldErrors?: Record<string, string[]>
    emailSent?: boolean
    emailError?: string
}

export async function inviteEmployee(data: z.infer<typeof inviteSchema>) {
    const cookieStore = cookies()
    const supabase = await createClient();

    const validatedFields = inviteSchema.safeParse(data)

    if (!validatedFields.success) {
        return {
            success: false,
            error: 'Invalid fields',
            fieldErrors: validatedFields.error.flatten().fieldErrors,
        }
    }

    const { email, role, projectIds, permissions } = validatedFields.data

    try {
        // Get current user's company
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data: currentUserProfile, error: profileError } = await supabase
            .from('users')
            .select('company_id, role')
            .eq('id', user.id)
            .single()

        if (profileError || !currentUserProfile?.company_id) {
            throw new Error('Company not found')
        }

        if (currentUserProfile.role !== 'admin') {
            throw new Error('Only admins can invite employees')
        }

        // Check if invite already exists
        const { data: existingInvite } = await supabase
            .from('invites')
            .select('id')
            .eq('email', email)
            .eq('company_id', currentUserProfile.company_id)
            .single()

        if (existingInvite) {
            return { success: false, error: 'Invite already sent to this email' }
        }

        // Check if user already exists globally
        const { data: existingUser } = await supabase
            .from('users')
            .select('id, company_id')
            .eq('email', email)
            .single()

        if (existingUser) {
            if (existingUser.company_id === currentUserProfile.company_id) {
                return { success: false, error: 'User already exists in this company' }
            }
            if (existingUser.company_id) {
                return { success: false, error: 'User is already assigned to another company' }
            }
        }

        // Fetch company data to get the name
        const { data: companyData } = await supabase
            .from('companies')
            .select('name')
            .eq('id', currentUserProfile.company_id)
            .single();

        // Check for existing pending invite
        const { data: pendingInvite } = await supabase
            .from('invites')
            .select('id')
            .eq('email', email)
            .eq('company_id', currentUserProfile.company_id)
            .eq('status', 'pending')
            .single()

        if (pendingInvite) {
            return { success: false, error: 'User already has a pending invite for this company' }
        }

        // Create invite
        const { data: inviteData, error: insertError } = await supabase
            .from('invites')
            .insert({
                email,
                role,
                company_id: currentUserProfile.company_id,
                company_name: companyData?.name || 'Unknown Company',
                project_ids: projectIds,
                status: 'pending'
            })
            .select()
            .single()

        if (insertError) throw insertError

        // Trigger email sending via Edge Function
        let emailSent = false;
        let emailErrorMsg = undefined;

        try {
            const inviteLink = `${process.env.NEXT_PUBLIC_BASE_URL || 'http://localhost:3000'}/accept-invite?token=${inviteData.id}`;

            const { data: funcData, error: funcError } = await supabase.functions.invoke('send-invite-email', {
                body: {
                    email,
                    role,
                    companyName: companyData?.name || 'Constructor',
                    inviteLink,
                },
            })

            if (funcError) {
                console.error('Supabase Function Invoke Error:', funcError);
                throw funcError;
            }

            if (funcData && !funcData.success) {
                console.error('Email Sending Failed:', funcData.error);
                throw new Error(typeof funcData.error === 'string' ? funcData.error : JSON.stringify(funcData.error));
            }

            emailSent = true;

        } catch (emailError: any) {
            console.error('Failed to trigger email function:', emailError)
            emailErrorMsg = emailError.message || 'Failed to send email';
        }

        return { success: true, emailSent, emailError: emailErrorMsg }
    } catch (error: any) {
        console.error('Error inviting employee:', error)
        return { success: false, error: error.message || 'Failed to invite employee' }
    }
}

export async function getPendingInvites() {
    const cookieStore = cookies()
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) return []

        const { data: profile } = await supabase
            .from('users')
            .select('company_id')
            .eq('id', user.id)
            .single()

        if (!profile?.company_id) return []

        const { data: invites } = await supabase
            .from('invites')
            .select('*')
            .eq('company_id', profile.company_id)
            .eq('status', 'pending')
            .order('created_at', { ascending: false })

        return (invites || []).map(invite => ({
            id: invite.id,
            email: invite.email,
            companyId: invite.company_id,
            companyName: invite.company_name,
            role: invite.role,
            projectIds: invite.project_ids || [],
            status: invite.status,
            createdAt: invite.created_at,
        }))
    } catch (error) {
        console.error('Error fetching pending invites:', error)
        return []
    }
}

export async function getUserPendingInvites() {
    const cookieStore = cookies()
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()

        if (!user || !user.email) {
            return []
        }

        // Get all pending invites for this email
        const { data: invites, error } = await supabase
            .from('invites')
            .select('*, companies(name)')
            .eq('email', user.email)
            .eq('status', 'pending')

        if (error) {
            console.error('Error fetching pending invites:', error)
            return []
        }

        return (invites || []).map(invite => ({
            id: invite.id,
            email: invite.email,
            companyId: invite.company_id,
            companyName: invite.company_name || invite.companies?.name,
            role: invite.role,
            projectIds: invite.project_ids || [],
            status: invite.status,
            createdAt: invite.created_at,
            companies: invite.companies,
        }))
    } catch (error) {
        console.error('Error checking pending invites:', error)
        return []
    }
}

export async function acceptInvite(inviteId: string) {
    const cookieStore = cookies()
    const supabase = await createClient();

    try {
        console.log('[acceptInvite] Starting to accept invite:', inviteId)

        const { data: { user } } = await supabase.auth.getUser()
        console.log('[acceptInvite] User:', user?.email)

        if (!user || !user.email) throw new Error('Unauthorized')

        // Verify invite exists and belongs to user
        const { data: invite, error: inviteError } = await supabase
            .from('invites')
            .select('*')
            .eq('id', inviteId)
            .eq('email', user.email)
            .eq('status', 'pending')
            .single()

        console.log('[acceptInvite] Invite lookup:', { invite, inviteError })

        if (inviteError || !invite) throw new Error('Invite not found or invalid')

        // Upsert user record (create if doesn't exist, update if it does)
        console.log('[acceptInvite] Upserting user profile...')
        const { error: userError } = await supabase
            .from('users')
            .upsert({
                id: user.id,
                email: user.email,
                display_name: user.user_metadata?.name || user.user_metadata?.full_name || user.email?.split('@')[0],
                phone: user.user_metadata?.phone || null,
                photo_url: user.user_metadata?.avatar_url || user.user_metadata?.picture || null,
                company_id: invite.company_id,
                role: invite.role,
                permissions: invite.permissions || {},
                status: 'active',
                updated_at: new Date().toISOString()
            }, {
                onConflict: 'id'
            })

        console.log('[acceptInvite] User upsert result:', { userError })

        if (userError) throw userError

        // Insert project memberships
        if (invite.project_ids && invite.project_ids.length > 0) {
            const memberInserts = invite.project_ids.map((projectId: string) => ({
                project_id: projectId,
                user_id: user.id,
                role: invite.role || 'member'
            }));
            const { error: memberError } = await supabase
                .from('project_members')
                .insert(memberInserts);
            if (memberError) console.error('Error inserting project members:', memberError);
        }

        // Update invite status
        console.log('[acceptInvite] Updating invite status...')
        const { error: inviteUpdateError } = await supabase
            .from('invites')
            .update({ status: 'accepted' })
            .eq('id', inviteId)

        console.log('[acceptInvite] Invite update result:', { inviteUpdateError })

        if (inviteUpdateError) throw inviteUpdateError

        console.log('[acceptInvite] Success!')
        return { success: true }
    } catch (error: any) {
        console.error('[acceptInvite] Error:', error)
        return { success: false, error: error.message }
    }
}

export async function updateEmployee(employeeId: string, data: {
    role?: 'admin' | 'manager' | 'member';
    projectIds?: string[];
    permissions?: Record<string, any>;
}) {
    const cookieStore = cookies()
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        // Verify current user is admin
        const { data: currentUser } = await supabase
            .from('users')
            .select('role, company_id')
            .eq('id', user.id)
            .single()

        if (!currentUser || currentUser.role !== 'admin') {
            throw new Error('Only admins can update employees')
        }

        // Build update object
        const updateData: any = {
            updated_at: new Date().toISOString()
        }

        if (data.role) updateData.role = data.role
        if (data.permissions !== undefined) updateData.permissions = data.permissions

        // Update employee
        const { error } = await supabase
            .from('users')
            .update(updateData)
            .eq('id', employeeId)
            .eq('company_id', currentUser.company_id) // Ensure same company

        if (error) throw error

        if (data.projectIds !== undefined) {
            // Delete existing memberships
            const { error: deleteError } = await supabase
                .from('project_members')
                .delete()
                .eq('user_id', employeeId);
            if (deleteError) throw deleteError;

            // Insert new memberships
            if (data.projectIds.length > 0) {
                const memberInserts = data.projectIds.map((projectId: string) => ({
                    project_id: projectId,
                    user_id: employeeId,
                    role: data.role || 'member'
                }));
                const { error: insertError } = await supabase
                    .from('project_members')
                    .insert(memberInserts);
                if (insertError) throw insertError;
            }
        }

        if (error) throw error

        return { success: true }
    } catch (error: any) {
        console.error('Error updating employee:', error)
        return { success: false, error: error.message }
    }
}

export async function resignFromCompany() {
    const cookieStore = cookies()
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        // Fetch user profile for history logging
        const { data: userProfile } = await supabase
            .from('users')
            .select('*')
            .eq('id', user.id)
            .single()

        if (!userProfile?.company_id) throw new Error('Not in a company')

        // Log to employment_history before clearing
        const { error: historyError } = await supabase
            .from('employment_history')
            .insert({
                user_id: user.id,
                company_id: userProfile.company_id,
                role: userProfile.role,
                exit_reason: 'resigned',
                user_details: {
                    displayName: userProfile.display_name,
                    email: userProfile.email,
                    phone: userProfile.phone,
                    photoURL: userProfile.photo_url
                }
            })

        if (historyError) {
            console.error('Error logging employment history:', historyError)
        }

        // Clear company assignment
        const { error } = await supabase
            .from('users')
            .update({
                company_id: null,
                role: 'member',
                status: 'active',
                updated_at: new Date().toISOString()
            })
            .eq('id', user.id)

        if (error) throw error

        // Clear project memberships
        await supabase
            .from('project_members')
            .delete()
            .eq('user_id', user.id)

        if (error) throw error

        return { success: true }
    } catch (error: any) {
        console.error('Error resigning from company:', error)
        return { success: false, error: error.message }
    }
}

export async function getExEmployees() {
    const cookieStore = cookies()
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) return []

        // Check if admin
        const { data: currentUser } = await supabase
            .from('users')
            .select('role, company_id')
            .eq('id', user.id)
            .single()

        if (!currentUser || currentUser.role !== 'admin' || !currentUser.company_id) {
            return []
        }

        // Get currently active employees to filter them out of the ex-employees history
        const { data: activeUsers } = await supabase
            .from('users')
            .select('id')
            .eq('company_id', currentUser.company_id)

        const activeUserIds = new Set(activeUsers?.map(u => u.id) || [])

        const { data: history } = await supabase
            .from('employment_history')
            .select('*')
            .eq('company_id', currentUser.company_id)
            .order('exit_date', { ascending: false })

        const filteredHistory = (history || []).filter(h => !activeUserIds.has(h.user_id))

        return filteredHistory
    } catch (error) {
        console.error('Error fetching ex-employees:', error)
        return []
    }
}

export async function removeEmployee(employeeId: string) {
    const cookieStore = cookies()
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        // Verify current user is admin
        const { data: currentUser } = await supabase
            .from('users')
            .select('role, company_id')
            .eq('id', user.id)
            .single()

        if (!currentUser || currentUser.role !== 'admin' || !currentUser.company_id) {
            throw new Error('Only admins can remove employees')
        }

        // Get employee to remove
        const { data: employee } = await supabase
            .from('users')
            .select('*')
            .eq('id', employeeId)
            .eq('company_id', currentUser.company_id)
            .single()

        if (!employee) throw new Error('Employee not found')

        // Log to employment_history
        const { error: historyError } = await supabase
            .from('employment_history')
            .insert({
                user_id: employee.id,
                company_id: currentUser.company_id,
                role: employee.role,
                exit_reason: 'removed',
                user_details: {
                    displayName: employee.display_name,
                    email: employee.email,
                    phone: employee.phone,
                    photoURL: employee.photo_url
                }
            })

        if (historyError) console.error('Error logging history:', historyError)

        // Remove from company
        const { error } = await supabase
            .from('users')
            .update({
                company_id: null,
                role: 'member',
                status: 'active', // Reset to active so they can join elsewhere
                updated_at: new Date().toISOString()
            })
            .eq('id', employeeId)

        if (error) throw error

        // Clear project memberships
        await supabase
            .from('project_members')
            .delete()
            .eq('user_id', employeeId);

        return { success: true }
    } catch (error: any) {
        console.error('Error removing employee:', error)
        return { success: false, error: error.message }
    }
}

export type EmailStatusResult = {
    status: 'current' | 'ex' | 'new' | 'other_company';
    userId?: string;
    message?: string;
}

export async function checkEmailStatus(email: string): Promise<EmailStatusResult> {
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        const { data: currentUserProfile } = await supabase
            .from('users')
            .select('company_id')
            .eq('id', user.id)
            .single()

        if (!currentUserProfile?.company_id) {
            throw new Error('Company not found')
        }

        const companyId = currentUserProfile.company_id

        // Check if user already exists
        const { data: existingUser } = await supabase
            .from('users')
            .select('id, company_id')
            .ilike('email', email.trim())
            .maybeSingle()

        if (!existingUser) {
            return { status: 'new' }
        }

        if (existingUser.company_id === companyId) {
            return {
                status: 'current',
                message: 'This email belongs to a current employee of your company.'
            }
        }

        // Check if they are an ex-employee of this company
        const { data: history } = await supabase
            .from('employment_history')
            .select('id')
            .eq('user_id', existingUser.id)
            .eq('company_id', companyId)
            .limit(1)
            .maybeSingle()

        if (history) {
            return {
                status: 'ex',
                userId: existingUser.id,
                message: 'Ex-employee detected. Clicking Rejoin will add them back to the company immediately.'
            }
        }

        if (existingUser.company_id) {
            return {
                status: 'other_company',
                message: 'User is already assigned to another company.'
            }
        }

        return { status: 'new' }
    } catch (error) {
        console.error('Error checking email status:', error)
        return { status: 'new' }
    }
}

export async function rejoinEmployee(
    employeeId: string,
    data: {
        role: 'admin' | 'manager' | 'member';
        projectIds: string[];
        permissions: Record<string, any>;
    }
) {
    const supabase = await createClient();

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        // Verify current user is admin
        const { data: currentUser } = await supabase
            .from('users')
            .select('role, company_id')
            .eq('id', user.id)
            .single()

        if (!currentUser || currentUser.role !== 'admin' || !currentUser.company_id) {
            throw new Error('Only admins can rejoin employees')
        }

        const companyId = currentUser.company_id

        // Reactivate employee profile
        const { error: updateError } = await supabase
            .from('users')
            .update({
                company_id: companyId,
                role: data.role,
                permissions: data.permissions,
                status: 'active',
                updated_at: new Date().toISOString()
            })
            .eq('id', employeeId)

        if (updateError) throw updateError

        // Clear existing project memberships
        const { error: deleteError } = await supabase
            .from('project_members')
            .delete()
            .eq('user_id', employeeId)

        if (deleteError) throw deleteError

        // Insert new memberships
        if (data.projectIds && data.projectIds.length > 0) {
            const memberInserts = data.projectIds.map((projectId: string) => ({
                project_id: projectId,
                user_id: employeeId,
                role: data.role
            }))

            const { error: insertError } = await supabase
                .from('project_members')
                .insert(memberInserts)

            if (insertError) throw insertError
        }

        return { success: true }
    } catch (error: any) {
        console.error('Error rejoining employee:', error)
        return { success: false, error: error.message || 'Failed to rejoin employee' }
    }
}
