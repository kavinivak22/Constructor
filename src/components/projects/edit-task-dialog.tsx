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
import { Loader2, Pencil } from 'lucide-react';
import { editTaskAction, type TaskItem } from '@/app/actions/tasks';
import { useToast } from '@/hooks/use-toast';

interface EditTaskDialogProps {
    task: TaskItem;
    employees: { id: string; display_name: string; email: string }[];
    onSuccess: () => void;
    trigger?: React.ReactNode;
}

export function EditTaskDialog({ task, employees, onSuccess, trigger }: EditTaskDialogProps) {
    const { toast } = useToast();
    const [isOpen, setIsOpen] = useState(false);
    const [title, setTitle] = useState(task.title);
    const [assignedTo, setAssignedTo] = useState(task.assigned_to || '');
    const [dueDate, setDueDate] = useState(task.due_date || '');
    const [priority, setPriority] = useState(task.priority || 'medium');
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
        const result = await editTaskAction(task.id, {
            title: title.trim(),
            assignedTo,
            dueDate: dueDate || null,
            priority,
        });
        setIsSubmitting(false);

        if (result.success) {
            toast({
                title: 'Task Updated',
                description: 'Project task has been updated successfully.',
            });
            setIsOpen(false);
            onSuccess();
        } else {
            toast({
                title: 'Error',
                description: result.error || 'Failed to update project task.',
                variant: 'destructive',
            });
        }
    };

    return (
        <Dialog open={isOpen} onOpenChange={setIsOpen}>
            <DialogTrigger asChild>
                {trigger || (
                    <Button variant="ghost" size="icon" className="h-7 w-7 p-0">
                        <Pencil className="h-3.5 w-3.5" />
                    </Button>
                )}
            </DialogTrigger>
            <DialogContent className="sm:max-w-[425px]">
                <form onSubmit={handleSubmit}>
                    <DialogHeader>
                        <DialogTitle>Edit Project Task</DialogTitle>
                        <DialogDescription>
                            Update the details for this project task.
                        </DialogDescription>
                    </DialogHeader>
                    <div className="grid gap-4 py-4">
                        <div className="grid gap-2">
                            <Label htmlFor="edit-task-title">Task Title</Label>
                            <Input
                                id="edit-task-title"
                                value={title}
                                onChange={(e) => setTitle(e.target.value)}
                                placeholder="e.g. Complete electrical inspection"
                                required
                            />
                        </div>
                        <div className="grid gap-2">
                            <Label htmlFor="edit-assignee">Assign Employee</Label>
                            <Select onValueChange={setAssignedTo} value={assignedTo}>
                                <SelectTrigger id="edit-assignee">
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
                            <Label htmlFor="edit-due-date">Due Date</Label>
                            <Input
                                id="edit-due-date"
                                type="date"
                                value={dueDate}
                                onChange={(e) => setDueDate(e.target.value)}
                            />
                        </div>
                        <div className="grid gap-2">
                            <Label htmlFor="edit-priority">Priority</Label>
                            <Select onValueChange={setPriority} value={priority}>
                                <SelectTrigger id="edit-priority">
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
                            Save Changes
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
