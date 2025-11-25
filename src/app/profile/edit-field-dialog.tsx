'use client';

import { useState } from 'react';
import { useSupabase } from '@/supabase/provider';
import { Button } from '@/components/ui/button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import { Loader2, Edit, AlertCircle } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';

interface EditFieldDialogProps {
    field: 'displayName' | 'phone';
    label: string;
    currentValue: string;
    role: string;
    companyId: string;
    onUpdate: () => void;
}

export default function EditFieldDialog({
    field,
    label,
    currentValue,
    role,
    companyId,
    onUpdate,
}: EditFieldDialogProps) {
    const { supabase, user } = useSupabase();
    const { toast } = useToast();
    const [open, setOpen] = useState(false);
    const [value, setValue] = useState(currentValue);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const isAdmin = role === 'admin';
    const needsApproval = !isAdmin;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);

        try {
            if (needsApproval) {
                // Create change request
                const { error } = await supabase
                    .from('profile_change_requests')
                    .insert({
                        user_id: user?.id,
                        company_id: companyId,
                        field_name: field,
                        new_value: value,
                        status: 'pending'
                    });

                if (error) throw error;

                toast({
                    title: 'Request Sent',
                    description: `Your request to change ${label} has been sent to your admin for approval.`,
                });
            } else {
                // Direct update
                const { error } = await supabase
                    .from('users')
                    .update({ [field]: value })
                    .eq('id', user?.id);

                if (error) throw error;

                toast({
                    title: 'Success',
                    description: `${label} updated successfully.`,
                });
                onUpdate();
            }
            setOpen(false);
        } catch (error: any) {
            console.error('Error updating profile:', error);
            setError(error.message || 'Failed to update profile.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={(val) => {
            setOpen(val);
            if (!val) setError(null); // Clear error on close
        }}>
            <DialogTrigger asChild>
                <Button variant="ghost" size="icon" className="h-8 w-8">
                    <Edit className="h-4 w-4 text-muted-foreground" />
                </Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-[425px]">
                <DialogHeader>
                    <DialogTitle>
                        {needsApproval ? `Request ${label} Change` : `Edit ${label}`}
                    </DialogTitle>
                    <DialogDescription>
                        {needsApproval
                            ? `Submit a new ${label.toLowerCase()} for admin approval.`
                            : `Update your ${label.toLowerCase()} directly.`}
                    </DialogDescription>
                </DialogHeader>

                {error && (
                    <Alert variant="destructive" className="mb-4">
                        <AlertCircle className="h-4 w-4" />
                        <AlertTitle>Error</AlertTitle>
                        <AlertDescription>
                            {error}
                        </AlertDescription>
                    </Alert>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="grid gap-4 py-4">
                        <div className="grid grid-cols-4 items-center gap-4">
                            <Label htmlFor="name" className="text-right">
                                {label}
                            </Label>
                            <Input
                                id="name"
                                value={value}
                                onChange={(e) => setValue(e.target.value)}
                                className="col-span-3"
                            />
                        </div>
                    </div>
                    <DialogFooter>
                        <Button type="submit" disabled={loading}>
                            {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                            {needsApproval ? 'Submit Request' : 'Save Changes'}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
