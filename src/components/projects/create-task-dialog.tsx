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
import { createProjectTask } from '@/app/actions/tasks';
import { useToast } from '@/hooks/use-toast';

interface CreateTaskDialogProps {
    projectId: string;
    employees: { id: string; display_name: string; email: string }[];
    onSuccess: () => void;
    trigger?: React.ReactNode;
}

export function CreateTaskDialog({ projectId, employees, onSuccess, trigger }: CreateTaskDialogProps) {
    const { toast } = useToast();
    const [isOpen, setIsOpen] = useState(false);
    const [title, setTitle] = useState('');
    const [assignedTo, setAssignedTo] = useState('');
    const [dueDate, setDueDate] = useState('');
    const [priority, setPriority] = useState('medium');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!title.trim()) return;
        if (!assignedTo) {
            toast({
                title: 'Required Field',
                description: 'Please assign an employee to this task.',
                variant: 'destructive',
            });
            return;
        }

        setIsSubmitting(true);
        const result = await createProjectTask({
            projectId,
            title: title.trim(),
            assignedTo,
            dueDate: dueDate || undefined,
            priority,
        });
        setIsSubmitting(false);

        if (result.success) {
            toast({
                title: 'Task Created',
                description: 'Project task has been added successfully and assigned.',
            });
            setTitle('');
            setAssignedTo('');
            setDueDate('');
            setPriority('medium');
            setIsOpen(false);
            onSuccess();
        } else {
            toast({
                title: 'Error',
                description: result.error || 'Failed to create project task.',
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
                        Add Task
                    </Button>
                )}
            </DialogTrigger>
            <DialogContent className="sm:max-w-[425px]">
                <form onSubmit={handleSubmit}>
                    <DialogHeader>
                        <DialogTitle>Add Project Task</DialogTitle>
                        <DialogDescription>
                            Create a shared task for this project and assign it to a team member.
                        </DialogDescription>
                    </DialogHeader>
                    <div className="grid gap-4 py-4">
                        <div className="grid gap-2">
                            <Label htmlFor="task-title">Task Title</Label>
                            <Input
                                id="task-title"
                                value={title}
                                onChange={(e) => setTitle(e.target.value)}
                                placeholder="e.g. Complete electrical inspection"
                                required
                            />
                        </div>
                        <div className="grid gap-2">
                            <Label htmlFor="assignee">Assign Employee</Label>
                            <Select onValueChange={setAssignedTo} value={assignedTo}>
                                <SelectTrigger id="assignee">
                                  <SelectValue placeholder="Select employee" />
                                </SelectTrigger>
                                <SelectContent>
                                    {employees.map((emp) => (
                                        <SelectItem key={emp.id} value={emp.id}>
                                            {emp.display_name} ({emp.email})
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                        <div className="grid gap-2">
                            <Label htmlFor="due-date">Due Date</Label>
                            <Input
                                id="due-date"
                                type="date"
                                value={dueDate}
                                onChange={(e) => setDueDate(e.target.value)}
                            />
                        </div>
                        <div className="grid gap-2">
                            <Label htmlFor="priority">Priority</Label>
                            <Select onValueChange={setPriority} value={priority}>
                                <SelectTrigger id="priority">
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
                            Create Task
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
