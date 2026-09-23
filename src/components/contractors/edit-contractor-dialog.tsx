'use client';

import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Loader2, Edit3 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from '@/components/ui/dialog';
import {
    Form,
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { useToast } from '@/hooks/use-toast';
import { updateContractor } from '@/app/actions/contractors';

const contractorSchema = z.object({
    name: z.string().min(1, "Name is required"),
    category: z.string().optional(),
    contactPerson: z.string().optional(),
    phone: z.string().min(1, "Phone is required"),
    email: z.string().email("Invalid email").optional().or(z.literal('')),
});

type ContractorFormValues = z.infer<typeof contractorSchema>;

interface EditContractorDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    contractor: {
        id: string;
        name: string;
        category?: string | null;
        contactPerson?: string | null;
        phone?: string | null;
        email?: string | null;
    } | null;
    onSuccess: (updatedContractor: any) => void;
}

export function EditContractorDialog({ open, onOpenChange, contractor, onSuccess }: EditContractorDialogProps) {
    const { toast } = useToast();
    const form = useForm<ContractorFormValues>({
        resolver: zodResolver(contractorSchema),
        defaultValues: {
            name: '',
            category: '',
            contactPerson: '',
            phone: '',
            email: '',
        },
    });

    useEffect(() => {
        if (contractor) {
            form.reset({
                name: contractor.name || '',
                category: contractor.category || '',
                contactPerson: contractor.contactPerson || '',
                phone: contractor.phone || '',
                email: contractor.email || '',
            });
        }
    }, [contractor, form]);

    const onSubmit = async (data: ContractorFormValues) => {
        if (!contractor) return;
        try {
            const result = await updateContractor(contractor.id, data);
            if (result.success) {
                toast({
                    title: "Contractor updated",
                    description: `${data.name} details have been updated.`,
                });
                onSuccess(result.data);
                onOpenChange(false);
            } else {
                toast({
                    variant: "destructive",
                    title: "Error",
                    description: result.error || "Failed to update contractor.",
                });
            }
        } catch (error) {
            toast({
                variant: "destructive",
                title: "Error",
                description: "Something went wrong while updating contractor.",
            });
        }
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[450px] w-[94vw] rounded-2xl glass-card border border-white/10">
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2 text-base sm:text-lg font-bold">
                        <Edit3 className="h-5 w-5 text-primary" /> Edit Contractor Details
                    </DialogTitle>
                    <DialogDescription className="text-xs sm:text-sm">
                        Update company name, category, contact person, or phone number.
                    </DialogDescription>
                </DialogHeader>
                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                        <FormField
                            control={form.control}
                            name="name"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel className="text-xs sm:text-sm">Contractor Name</FormLabel>
                                    <FormControl>
                                        <Input {...field} placeholder="e.g. ABC Construction" className="h-9 sm:h-10 text-xs sm:text-sm rounded-xl" />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name="category"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel className="text-xs sm:text-sm">Category / Trade</FormLabel>
                                    <FormControl>
                                        <Input {...field} placeholder="e.g. Masonry, Plumbing, Electrical" className="h-9 sm:h-10 text-xs sm:text-sm rounded-xl" />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name="contactPerson"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel className="text-xs sm:text-sm">Contact Person</FormLabel>
                                    <FormControl>
                                        <Input {...field} placeholder="e.g. John Doe" className="h-9 sm:h-10 text-xs sm:text-sm rounded-xl" />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                            <FormField
                                control={form.control}
                                name="phone"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel className="text-xs sm:text-sm">Phone Number</FormLabel>
                                        <FormControl>
                                            <Input {...field} placeholder="Required" className="h-9 sm:h-10 text-xs sm:text-sm rounded-xl" />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="email"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel className="text-xs sm:text-sm">Email Address</FormLabel>
                                        <FormControl>
                                            <Input {...field} placeholder="Optional" className="h-9 sm:h-10 text-xs sm:text-sm rounded-xl" />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>
                        <DialogFooter className="gap-2 sm:gap-0 pt-2">
                            <Button type="button" variant="outline" size="sm" onClick={() => onOpenChange(false)} className="h-9 text-xs sm:text-sm rounded-xl">
                                Cancel
                            </Button>
                            <Button type="submit" size="sm" disabled={form.formState.isSubmitting} className="h-9 text-xs sm:text-sm rounded-xl">
                                {form.formState.isSubmitting && <Loader2 className="mr-1.5 h-4 w-4 animate-spin" />}
                                Update Details
                            </Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
}
