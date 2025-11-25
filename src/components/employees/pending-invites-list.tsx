import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Mail, Clock } from "lucide-react";
import { formatDistanceToNow } from "date-fns";

export interface PendingInvite {
    id: string;
    email: string;
    role: string;
    createdAt: string;
    status: string;
}

interface PendingInvitesListProps {
    invites: PendingInvite[];
}

export function PendingInvitesList({ invites }: PendingInvitesListProps) {
    if (invites.length === 0) return null;

    return (
        <Card className="mt-6">
            <CardHeader>
                <CardTitle className="text-lg font-semibold">Pending Invites</CardTitle>
            </CardHeader>
            <CardContent>
                <div className="space-y-3">
                    {invites.map((invite) => (
                        <div key={invite.id} className="flex items-start justify-between p-3 sm:p-4 border rounded-lg gap-3">
                            <div className="flex items-start gap-3 min-w-0 flex-1">
                                <div className="p-2 bg-primary/10 rounded-full shrink-0">
                                    <Mail className="w-4 h-4 text-primary" />
                                </div>
                                <div className="min-w-0 flex-1">
                                    <p className="font-medium text-sm sm:text-base truncate">{invite.email}</p>
                                    <div className="flex flex-wrap items-center gap-2 text-xs sm:text-sm text-muted-foreground mt-1">
                                        <Badge variant="outline" className="capitalize text-xs">
                                            {invite.role}
                                        </Badge>
                                        <span className="flex items-center gap-1 shrink-0">
                                            <Clock className="w-3 h-3" />
                                            <span className="hidden sm:inline">{formatDistanceToNow(new Date(invite.createdAt), { addSuffix: true })}</span>
                                            <span className="sm:hidden">{formatDistanceToNow(new Date(invite.createdAt), { addSuffix: true }).replace('about ', '')}</span>
                                        </span>
                                    </div>
                                </div>
                            </div>
                            <Badge variant="secondary" className="shrink-0 text-xs">Pending</Badge>
                        </div>
                    ))}
                </div>
            </CardContent>
        </Card>
    );
}
