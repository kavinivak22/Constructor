'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { getUserPendingInvites, acceptInvite } from '@/app/actions/employees'
import { useSupabase } from '@/supabase/provider'
import { Button } from '@/components/ui/button'
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Loader2 } from 'lucide-react'

export function InviteCheckWrapper() {
    const router = useRouter()
    const { supabase } = useSupabase()
    const [invites, setInvites] = useState<any[]>([])
    const [loading, setLoading] = useState(true)
    const [acceptingId, setAcceptingId] = useState<string | null>(null)
    const [showNoInviteDialog, setShowNoInviteDialog] = useState(false)

    useEffect(() => {
        checkInvites()
    }, [])

    async function checkInvites() {
        const pendingInvites = await getUserPendingInvites()

        setInvites(pendingInvites)
        setLoading(false)

        // If no invites, show the "no invite" dialog
        if (!pendingInvites || pendingInvites.length === 0) {
            setShowNoInviteDialog(true)
        }
    }

    async function handleAccept(inviteId: string) {
        setAcceptingId(inviteId)
        const result = await acceptInvite(inviteId)

        if (result.success) {
            // Force a refresh to ensure everything updates
            router.refresh()
            router.push('/')
        } else {
            alert('Failed to accept invite: ' + (result.error || 'Unknown error'))
            setAcceptingId(null)
        }
    }

    function handleDeclineInvite() {
        router.push('/register-company')
    }

    function handleCreateOwnCompany() {
        router.push('/register-company')
    }

    async function handleLogout() {
        await supabase.auth.signOut()
        router.push('/login')
    }

    if (loading) {
        return (
            <div className="fixed inset-0 bg-background flex items-center justify-center z-[9999]">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
        )
    }

    // Show "No Invite" dialog
    if (showNoInviteDialog && invites.length === 0) {
        return (
            <AlertDialog open={true}>
                <AlertDialogContent className="z-[9999]">
                    <AlertDialogHeader>
                        <AlertDialogTitle className="text-2xl">No Company Invitation Found</AlertDialogTitle>
                        <AlertDialogDescription className="text-base pt-2">
                            You haven't been invited to join any company yet.
                            <br /><br />
                            You can either <strong>create your own company</strong> to get started, or <strong>contact an administrator</strong> to request an invitation to an existing company.
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={handleLogout}>
                            Log Out
                        </AlertDialogCancel>
                        <AlertDialogAction onClick={handleCreateOwnCompany}>
                            Create My Own Company
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        )
    }

    // Show "Multiple Invites" or "Single Invite" dialog
    if (invites.length > 0) {
        // If multiple invites, show list
        if (invites.length > 1) {
            return (
                <AlertDialog open={true}>
                    <AlertDialogContent className="z-[9999] max-w-md">
                        <AlertDialogHeader>
                            <AlertDialogTitle className="text-2xl">You have {invites.length} pending invitations!</AlertDialogTitle>
                            <AlertDialogDescription className="text-base pt-2">
                                Please select a company to join:
                            </AlertDialogDescription>
                        </AlertDialogHeader>
                        <div className="flex flex-col gap-3 py-4">
                            {invites.map((invite) => (
                                <div key={invite.id} className="flex items-center justify-between p-3 border rounded-lg bg-card hover:bg-accent/50 transition-colors">
                                    <div>
                                        <div className="font-semibold">{invite.companyName || 'Unknown Company'}</div>
                                        <div className="text-sm text-muted-foreground capitalize">{invite.role}</div>
                                    </div>
                                    <Button
                                        onClick={() => handleAccept(invite.id)}
                                        disabled={!!acceptingId}
                                        size="sm"
                                    >
                                        {acceptingId === invite.id ? (
                                            <Loader2 className="h-4 w-4 animate-spin" />
                                        ) : (
                                            'Join'
                                        )}
                                    </Button>
                                </div>
                            ))}
                        </div>
                        <AlertDialogFooter>
                            <AlertDialogCancel onClick={handleLogout}>
                                Log Out
                            </AlertDialogCancel>
                            <AlertDialogAction onClick={handleCreateOwnCompany}>
                                Create My Own Company
                            </AlertDialogAction>
                        </AlertDialogFooter>
                    </AlertDialogContent>
                </AlertDialog>
            )
        }

        // Single invite (existing behavior)
        const invite = invites[0]
        const companyName = invite.companyName || 'the company'
        const role = invite.role === 'admin' ? 'an Administrator' :
            invite.role === 'manager' ? 'a Manager' : 'a Team Member'

        return (
            <AlertDialog open={true}>
                <AlertDialogContent className="z-[9999]">
                    <AlertDialogHeader>
                        <AlertDialogTitle className="text-2xl">You've been invited to join {companyName}!</AlertDialogTitle>
                        <AlertDialogDescription className="text-base pt-2">
                            <strong>{companyName}</strong> has invited you to join their team as {role}.
                            <br /><br />
                            Would you like to accept this invitation, or create your own company instead?
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={handleDeclineInvite} disabled={!!acceptingId}>
                            Create My Own Company
                        </AlertDialogCancel>
                        <AlertDialogAction onClick={() => handleAccept(invite.id)} disabled={!!acceptingId}>
                            {acceptingId === invite.id ? (
                                <>
                                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                                    Joining...
                                </>
                            ) : (
                                `Join ${companyName}`
                            )}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        )
    }

    return null
}
