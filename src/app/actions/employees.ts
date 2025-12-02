'use server'

import { createServerActionClient } from '@supabase/auth-helpers-nextjs'
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
    const supabase = createServerActionClient({ cookies: () => cookieStore })

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
            .select('companyId, role')
            .eq('id', user.id)
            .single()

        if (profileError || !currentUserProfile?.companyId) {
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
            .eq('companyId', currentUserProfile.companyId)
            .single()

        if (existingInvite) {
            return { success: false, error: 'Invite already sent to this email' }
        }

        // Check if user already exists globally
        const { data: existingUser } = await supabase
            .from('users')
            .select('id, companyId')
            .eq('email', email)
            .single()

        if (existingUser) {
            if (existingUser.companyId === currentUserProfile.companyId) {
                return { success: false, error: 'User already exists in this company' }
            }
            if (existingUser.companyId) {
                return { success: false, error: 'User is already assigned to another company' }
            }
        }

        // Fetch company data to get the name
        const { data: companyData } = await supabase
            .from('companies')
            .select('name')
            .eq('id', currentUserProfile.companyId)
            .single();

        // Check for existing pending invite
        const { data: pendingInvite } = await supabase
            .from('invites')
            .select('id')
            .eq('email', email)
            .eq('companyId', currentUserProfile.companyId)
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
                "companyId": currentUserProfile.companyId,
                "companyName": companyData?.name || 'Unknown Company',
                "projectIds": projectIds,
                permissions: permissions || {},
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
    const supabase = createServerActionClient({ cookies: () => cookieStore })

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) return []

        const { data: profile } = await supabase
            .from('users')
            .select('companyId')
            .eq('id', user.id)
            .single()

        if (!profile?.companyId) return []

        const { data: invites } = await supabase
            .from('invites')
            .select('*')
            .eq('companyId', profile.companyId)
            .eq('status', 'pending')
            .order('createdAt', { ascending: false })

        return invites || []
    } catch (error) {
        console.error('Error fetching pending invites:', error)
        return []
    }
}

export async function getUserPendingInvites() {
    const cookieStore = cookies()
    const supabase = createServerActionClient({ cookies: () => cookieStore })

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

        return invites || []
    } catch (error) {
        console.error('Error checking pending invites:', error)
        return []
    }
}

export async function acceptInvite(inviteId: string) {
    const cookieStore = cookies()
    const supabase = createServerActionClient({ cookies: () => cookieStore })

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
                displayName: user.user_metadata?.name || user.user_metadata?.full_name || user.email?.split('@')[0],
                phone: user.user_metadata?.phone || null,
                photoURL: user.user_metadata?.avatar_url || user.user_metadata?.picture || null,
                companyId: invite.companyId,
                role: invite.role,
                projectIds: invite.projectIds || [],
                permissions: invite.permissions || {},
                status: 'active',
                updated_at: new Date().toISOString()
            }, {
                onConflict: 'id'
            })

        console.log('[acceptInvite] User upsert result:', { userError })

        if (userError) throw userError

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
    const supabase = createServerActionClient({ cookies: () => cookieStore })

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        // Verify current user is admin
        const { data: currentUser } = await supabase
            .from('users')
            .select('role, companyId')
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
        if (data.projectIds !== undefined) updateData.projectIds = data.projectIds
        if (data.permissions !== undefined) updateData.permissions = data.permissions

        // Update employee
        const { error } = await supabase
            .from('users')
            .update(updateData)
            .eq('id', employeeId)
            .eq('companyId', currentUser.companyId) // Ensure same company

        if (error) throw error

        return { success: true }
    } catch (error: any) {
        console.error('Error updating employee:', error)
        return { success: false, error: error.message }
    }
}

export async function resignFromCompany() {
    const cookieStore = cookies()
    const supabase = createServerActionClient({ cookies: () => cookieStore })

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        // Fetch user profile for history logging
        const { data: userProfile } = await supabase
            .from('users')
            .select('*')
            .eq('id', user.id)
            .single()

        if (!userProfile?.companyId) throw new Error('Not in a company')

        // Log to employment_history before clearing
        const { error: historyError } = await supabase
            .from('employment_history')
            .insert({
                user_id: user.id,
                company_id: userProfile.companyId,
                role: userProfile.role,
                exit_reason: 'resigned',
                user_details: {
                    displayName: userProfile.displayName,
                    email: userProfile.email,
                    phone: userProfile.phone,
                    photoURL: userProfile.photoURL
                }
            })

        if (historyError) {
            console.error('Error logging employment history:', historyError)
        }

        // Clear company assignment
        const { error } = await supabase
            .from('users')
            .update({
                companyId: null,
                role: 'member',
                projectIds: [],
                status: 'active',
                updated_at: new Date().toISOString()
            })
            .eq('id', user.id)

        if (error) throw error

        return { success: true }
    } catch (error: any) {
        console.error('Error resigning from company:', error)
        return { success: false, error: error.message }
    }
}

export async function getExEmployees() {
    const cookieStore = cookies()
    const supabase = createServerActionClient({ cookies: () => cookieStore })

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) return []

        // Check if admin
        const { data: currentUser } = await supabase
            .from('users')
            .select('role, companyId')
            .eq('id', user.id)
            .single()

        if (!currentUser || currentUser.role !== 'admin' || !currentUser.companyId) {
            return []
        }

        const { data: history } = await supabase
            .from('employment_history')
            .select('*')
            .eq('company_id', currentUser.companyId)
            .order('exit_date', { ascending: false })

        return history || []
    } catch (error) {
        console.error('Error fetching ex-employees:', error)
        return []
    }
}

export async function removeEmployee(employeeId: string) {
    const cookieStore = cookies()
    const supabase = createServerActionClient({ cookies: () => cookieStore })

    try {
        const { data: { user } } = await supabase.auth.getUser()
        if (!user) throw new Error('Unauthorized')

        // Verify current user is admin
        const { data: currentUser } = await supabase
            .from('users')
            .select('role, companyId')
            .eq('id', user.id)
            .single()

        if (!currentUser || currentUser.role !== 'admin' || !currentUser.companyId) {
            throw new Error('Only admins can remove employees')
        }

        // Get employee to remove
        const { data: employee } = await supabase
            .from('users')
            .select('*')
            .eq('id', employeeId)
            .eq('companyId', currentUser.companyId)
            .single()

        if (!employee) throw new Error('Employee not found')

        // Log to employment_history
        const { error: historyError } = await supabase
            .from('employment_history')
            .insert({
                user_id: employee.id,
                company_id: currentUser.companyId,
                role: employee.role,
                exit_reason: 'removed',
                user_details: {
                    displayName: employee.displayName,
                    email: employee.email,
                    phone: employee.phone,
                    photoURL: employee.photoURL
                }
            })

        if (historyError) console.error('Error logging history:', historyError)

        // Remove from company
        const { error } = await supabase
            .from('users')
            .update({
                companyId: null,
                role: 'member',
                projectIds: [],
                status: 'active', // Reset to active so they can join elsewhere
                updated_at: new Date().toISOString()
            })
            .eq('id', employeeId)

        if (error) throw error

        return { success: true }
    } catch (error: any) {
        console.error('Error removing employee:', error)
        return { success: false, error: error.message }
    }
}
