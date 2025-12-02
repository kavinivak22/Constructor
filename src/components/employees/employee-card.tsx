

'use client';

import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card";
import { Switch } from "@/components/ui/switch";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import { type Project, type User as AppUser } from "@/lib/data";
import { cn } from "@/lib/utils";
import { Edit, FolderKanban, Mail, Phone, UserMinus } from "lucide-react";
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogTrigger,
} from "@/components/ui/alert-dialog"

interface EmployeeCardProps {
    employee: AppUser;
    projects: Project[];
    onEdit?: (employee: AppUser) => void;
    onStatusChange?: (employee: AppUser, newStatus: 'active' | 'inactive') => void;
    onRemove?: (employee: AppUser) => void;
    isCurrentUser: boolean;
}

export function EmployeeCard({ employee, projects, onEdit, onStatusChange, onRemove, isCurrentUser }: EmployeeCardProps) {

    const roleVariant: { [key: string]: 'default' | 'secondary' | 'outline' } = {
        admin: 'default',
        manager: 'secondary',
        member: 'outline',
    };

    const isInteractive = !!onEdit && !!onStatusChange;

    return (
        <Card className={cn("flex flex-col", employee.status === 'inactive' && "bg-muted/50")}>
            <CardHeader className="flex-row items-start gap-4">
                <Avatar className="w-16 h-16 border">
                    <AvatarImage src={employee.photoURL} alt={employee.displayName} />
                    <AvatarFallback>{employee.displayName.charAt(0)}</AvatarFallback>
                </Avatar>
                <div className="flex-1">
                    <div className="flex items-center justify-between">
                        <h3 className="text-lg font-bold font-headline">{employee.displayName}</h3>
                        {isInteractive && (
                            <Button variant="ghost" size="icon" className="w-8 h-8" onClick={() => onEdit(employee)}>
                                <Edit className="w-4 h-4" />
                            </Button>
                        )}
                    </div>
                    <Badge variant={roleVariant[employee.role] || 'outline'} className="mt-1 capitalize">{employee.role}</Badge>
                </div>
            </CardHeader>
            <CardContent className="flex-grow space-y-4">
                <div className="space-y-2 text-sm">
                    <div className="flex items-center gap-2 min-w-0">
                        <Mail className="w-4 h-4 text-muted-foreground shrink-0" />
                        <span className="truncate">{employee.email}</span>
                    </div>
                    {employee.phone && (
                        <div className="flex items-center gap-2 min-w-0">
                            <Phone className="w-4 h-4 text-muted-foreground shrink-0" />
                            <span className="truncate">{employee.phone}</span>
                        </div>
                    )}
                </div>
                <div className="space-y-2">
                    <div className="flex items-center gap-2 text-sm font-semibold">
                        <FolderKanban className="w-4 h-4 text-muted-foreground" />
                        <span>Assigned Projects</span>
                    </div>
                    {projects.length > 0 ? (
                        <div className="flex flex-wrap gap-2">
                            <TooltipProvider>
                                {projects.map(project => (
                                    <Tooltip key={project.id}>
                                        <TooltipTrigger asChild>
                                            <Badge variant="secondary" className="max-w-[100px] truncate">
                                                {project.name}
                                            </Badge>
                                        </TooltipTrigger>
                                        <TooltipContent>
                                            <p>{project.name}</p>
                                        </TooltipContent>
                                    </Tooltip>
                                ))}
                            </TooltipProvider>
                        </div>
                    ) : (
                        <p className="text-sm text-muted-foreground">No projects assigned.</p>
                    )}
                </div>
            </CardContent>
            {isInteractive && (
                <CardFooter className="border-t pt-4 flex items-center justify-between">
                    <div className="flex items-center gap-2">
                        <Switch
                            checked={employee.status === 'active'}
                            onCheckedChange={(checked) => onStatusChange(employee, checked ? 'active' : 'inactive')}
                            disabled={isCurrentUser}
                            aria-label={`Toggle status for ${employee.displayName}`}
                        />
                        <span className={cn(
                            "text-sm font-semibold",
                            employee.status === 'active' ? 'text-green-600' : 'text-red-600'
                        )}>
                            {employee.status === 'active' ? 'Active' : 'Inactive'}
                        </span>
                    </div>

                    {!isCurrentUser && (
                        <AlertDialog>
                            <AlertDialogTrigger asChild>
                                <Button variant="ghost" size="sm" className="text-destructive hover:text-destructive hover:bg-destructive/10">
                                    <UserMinus className="w-4 h-4 mr-1" />
                                    Remove
                                </Button>
                            </AlertDialogTrigger>
                            <AlertDialogContent>
                                <AlertDialogHeader>
                                    <AlertDialogTitle>Remove Employee?</AlertDialogTitle>
                                    <AlertDialogDescription>
                                        Are you sure you want to remove <b>{employee.displayName}</b> from the company?
                                        This action cannot be undone. They will lose access to all company data.
                                    </AlertDialogDescription>
                                </AlertDialogHeader>
                                <AlertDialogFooter>
                                    <AlertDialogCancel>Cancel</AlertDialogCancel>
                                    <AlertDialogAction
                                        onClick={() => onRemove?.(employee)}
                                        className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                                    >
                                        Remove
                                    </AlertDialogAction>
                                </AlertDialogFooter>
                            </AlertDialogContent>
                        </AlertDialog>
                    )}
                </CardFooter>
            )}
        </Card>
    );
}
