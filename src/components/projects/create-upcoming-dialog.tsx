'use client';

import { useState } from 'react';
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
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import { Plus, Loader2 } from 'lucide-react';
import { createUpcomingWork } from '@/app/actions/tasks';
import { useToast } from '@/hooks/use-toast';

interface CreateUpcomingDialogProps {
    projectId: string;
    onSuccess: () => void;
    trigger?: React.ReactNode;
}

export function CreateUpcomingDialog({ projectId, onSuccess, trigger }: CreateUpcomingDialogProps) {
    const { toast } = useToast();
    const [isOpen, setIsOpen] = useState(false);
    const [title, setTitle] = useState('');
    const [dueDate, setDueDate] = useState('');
    const [duration, setDuration] = useState('');
    const [priority, setPriority] = useState('medium');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!title.trim() || !dueDate || !duration.trim()) return;

        setIsSubmitting(true);
        const result = await createUpcomingWork({
            projectId,
            title: title.trim(),
            dueDate,
            duration: duration.trim(),
            priority,
        });
        setIsSubmitting(false);

        if (result.success) {
            toast({
                title: 'Upcoming Work Created',
                description: 'Upcoming work plan has been added successfully.',
            });
            setTitle('');
            setDueDate('');
            setDuration('');
            setPriority('medium');
            setIsOpen(false);
            onSuccess();
        } else {
            toast({
                title: 'Error',
                description: result.error || 'Failed to create upcoming work plan.',
                variant: 'destructive',
            });
        }
    };

    return (
        <Dialog open={isOpen} onOpenChange={setIsOpen}>
            <DialogTrigger asChild>
                {trigger || (
                    <Button variant="outline" className="w-full mt-4">
                        <Plus className="mr-2 h-4 w-4" />
                        Add Upcoming Work
                    </Button>
                )}
            </DialogTrigger>
            <DialogContent className="sm:max-w-[425px]">
                <form onSubmit={handleSubmit}>
                    <DialogHeader>
                        <DialogTitle>Add Upcoming Work Plan</DialogTitle>
                        <DialogDescription>
                            Plan a future phase or milestone for this project.
                        </DialogDescription>
                    </DialogHeader>
                    <div className="grid gap-4 py-4">
                        <div className="grid gap-2">
                            <Label htmlFor="work-title">Work Phase / Title</Label>
                            <Input
                                id="work-title"
                                value={title}
                                onChange={(e) => setTitle(e.target.value)}
                                placeholder="e.g. Roofing Installation"
                                required
                            />
                        </div>
                        <div className="grid gap-2">
                            <Label htmlFor="start-date">Estimated Start Date</Label>
                            <Input
                                id="start-date"
                                type="date"
                                value={dueDate}
                                onChange={(e) => setDueDate(e.target.value)}
                                required
                            />
                        </div>
                        <div className="grid gap-2">
                            <Label htmlFor="duration">Estimated Duration</Label>
                            <Input
                                id="duration"
                                value={duration}
                                onChange={(e) => setDuration(e.target.value)}
                                placeholder="e.g. 2 weeks"
                                required
                            />
                        </div>
                        <div className="grid gap-2">
                            <Label htmlFor="upcoming-priority">Priority</Label>
                            <Select onValueChange={setPriority} value={priority}>
                                <SelectTrigger id="upcoming-priority">
                                    <SelectValue placeholder="Select priority" />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="low">Low</SelectItem>
                                    <SelectItem value="medium">Medium</SelectItem>
                                    <SelectItem value="high">High</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>
                    </div>
                    <DialogFooter>
                        <Button type="submit" disabled={isSubmitting}>
                            {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                            Create Plan
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
