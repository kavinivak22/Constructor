'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Loader2, Plus } from 'lucide-react';
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
import { useToast } from '@/components/ui/use-toast';
import { createContractor } from '@/app/actions/contractors';

const contractorSchema = z.object({
    name: z.string().min(1, "Name is required"),
    category: z.string().optional(),
    contactPerson: z.string().optional(),
    phone: z.string().min(1, "Phone is required"),
    email: z.string().email("Invalid email").optional().or(z.literal('')),
});

type ContractorFormValues = z.infer<typeof contractorSchema>;

interface CreateContractorDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    onSuccess: (contractor: any) => void;
}

export function CreateContractorDialog({ open, onOpenChange, onSuccess }: CreateContractorDialogProps) {
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

    const onSubmit = async (data: ContractorFormValues) => {
        try {
            const result = await createContractor(data);
            if (result.success) {
                toast({
                    title: "Contractor added",
                    description: `${data.name} has been successfully added.`,
                });
                form.reset();
                onSuccess(result.data);
                onOpenChange(false);
            } else {
                toast({
                    variant: "destructive",
                    title: "Error",
                    description: result.error || "Failed to add contractor.",
                });
            }
        } catch (error) {
            toast({
                variant: "destructive",
                title: "Error",
                description: "Something went wrong.",
            });
        }
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[425px]">
                <DialogHeader>
                    <DialogTitle>Add New Contractor</DialogTitle>
                    <DialogDescription>
                        Add a new contractor to your company list.
                    </DialogDescription>
                </DialogHeader>
                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                        <FormField
                            control={form.control}
                            name="name"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Contractor Name</FormLabel>
                                    <FormControl>
                                        <Input {...field} placeholder="e.g. ABC Construction" />
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
                                    <FormLabel>Category</FormLabel>
                                    <FormControl>
                                        <Input {...field} placeholder="e.g. Masonry, Plumbing" />
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
                                    <FormLabel>Contact Person</FormLabel>
                                    <FormControl>
                                        <Input {...field} placeholder="e.g. John Doe" />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <div className="grid grid-cols-2 gap-4">
                            <FormField
                                control={form.control}
                                name="phone"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Phone</FormLabel>
                                        <FormControl>
                                            <Input {...field} placeholder="Required" />
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
                                        <FormLabel>Email</FormLabel>
                                        <FormControl>
                                            <Input {...field} placeholder="Optional" />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>
                        <DialogFooter>
                            <Button type="submit" disabled={form.formState.isSubmitting}>
                                {form.formState.isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                                Save Contractor
                            </Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
}
