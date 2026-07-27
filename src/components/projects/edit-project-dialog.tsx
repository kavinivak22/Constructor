'use client';

import { useState, useEffect } from 'react';
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
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Edit, Loader2 } from 'lucide-react';
import { useUpdateProject } from '@/hooks/queries/use-projects';
import { type Project } from '@/lib/data';

interface EditProjectDialogProps {
  project: Project;
  trigger?: React.ReactNode;
  onSuccess?: () => void;
}

export function EditProjectDialog({ project, trigger, onSuccess }: EditProjectDialogProps) {
  const [isOpen, setIsOpen] = useState(false);
  const updateProjectMutation = useUpdateProject();

  const [name, setName] = useState(project.name || '');
  const [description, setDescription] = useState(project.description || '');
  const [clientName, setClientName] = useState(project.client_name || project.clientName || '');
  const [location, setLocation] = useState(project.location || '');
  const [status, setStatus] = useState(project.status || 'in-progress');
  const [progress, setProgress] = useState<number>(project.progress ?? 0);
  const [budget, setBudget] = useState<number | ''>(project.budget ?? '');
  const [startDate, setStartDate] = useState(project.start_date || project.startDate || '');
  const [endDate, setEndDate] = useState(project.end_date || project.endDate || '');

  useEffect(() => {
    if (project) {
      setName(project.name || '');
      setDescription(project.description || '');
      setClientName(project.client_name || project.clientName || '');
      setLocation(project.location || '');
      setStatus(project.status || 'in-progress');
      setProgress(project.progress ?? 0);
      setBudget(project.budget ?? '');
      setStartDate((project.start_date || project.startDate || '').split('T')[0]);
      setEndDate((project.end_date || project.endDate || '').split('T')[0]);
    }
  }, [project, isOpen]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    updateProjectMutation.mutate(
      {
        id: project.id,
        name: name.trim(),
        description: description.trim() || undefined,
        clientName: clientName.trim() || undefined,
        location: location.trim() || undefined,
        status: status as any,
        progress: Number(progress),
        budget: budget !== '' ? Number(budget) : undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
      },
      {
        onSuccess: () => {
          setIsOpen(false);
          if (onSuccess) onSuccess();
        },
      }
    );
  };

  return (
    <Dialog open={isOpen} onOpenChange={setIsOpen}>
      <DialogTrigger asChild>
        {trigger || (
          <Button variant="outline" size="sm">
            <Edit className="mr-2 h-3.5 w-3.5" />
            Edit Project
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="sm:max-w-[550px] max-h-[90vh] overflow-y-auto">
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>Edit Project Details</DialogTitle>
            <DialogDescription>
              Update project information, timeline, progress, and budget.
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 py-4 text-sm">
            <div className="grid gap-2">
              <Label htmlFor="proj-name">Project Name</Label>
              <Input
                id="proj-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Villa Construction Site A"
                required
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="client-name">Client Name</Label>
                <Input
                  id="client-name"
                  value={clientName}
                  onChange={(e) => setClientName(e.target.value)}
                  placeholder="e.g. Apex Builders"
                />
              </div>

              <div className="grid gap-2">
                <Label htmlFor="proj-location">Location</Label>
                <Input
                  id="proj-location"
                  value={location}
                  onChange={(e) => setLocation(e.target.value)}
                  placeholder="e.g. Sector 62, Gurgaon"
                />
              </div>
            </div>

            <div className="grid gap-2">
              <Label htmlFor="proj-description">Description / Notes</Label>
              <Textarea
                id="proj-description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Key scope of work, targets..."
                rows={3}
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="proj-status">Status</Label>
                <Select onValueChange={setStatus} value={status}>
                  <SelectTrigger id="proj-status">
                    <SelectValue placeholder="Select status" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="planning">Planning</SelectItem>
                    <SelectItem value="in-progress">In Progress / Active</SelectItem>
                    <SelectItem value="on-hold">On Hold</SelectItem>
                    <SelectItem value="completed">Completed</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="grid gap-2">
                <Label htmlFor="proj-progress">Progress (%)</Label>
                <Input
                  id="proj-progress"
                  type="number"
                  min={0}
                  max={100}
                  value={progress}
                  onChange={(e) => setProgress(Math.min(100, Math.max(0, Number(e.target.value))))}
                />
              </div>

              <div className="grid gap-2">
                <Label htmlFor="proj-budget">Budget (₹)</Label>
                <Input
                  id="proj-budget"
                  type="number"
                  min={0}
                  value={budget}
                  onChange={(e) => setBudget(e.target.value === '' ? '' : Number(e.target.value))}
                  placeholder="e.g. 850000"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="start-date">Start Date</Label>
                <Input
                  id="start-date"
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                />
              </div>

              <div className="grid gap-2">
                <Label htmlFor="end-date">End Date</Label>
                <Input
                  id="end-date"
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                />
              </div>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" type="button" onClick={() => setIsOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={updateProjectMutation.isPending}>
              {updateProjectMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Save Changes
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
