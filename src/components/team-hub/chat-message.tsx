'use client';

import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { cn } from '@/lib/utils';
import { formatDistanceToNow } from 'date-fns';
import { Timestamp } from 'firebase/firestore';


export interface ChatMessageProps {
  message: string;
  timestamp: Timestamp;
  user: {
    name: string;
    avatarUrl: string;
  };
  isCurrentUser: boolean;
}

export function ChatMessage({ message, timestamp, user, isCurrentUser }: ChatMessageProps) {
  const timeAgo = timestamp ? formatDistanceToNow(timestamp.toDate(), { addSuffix: true }) : 'just now';

  return (
    <div
      className={cn(
        'flex items-start gap-3',
        isCurrentUser ? 'flex-row-reverse' : ''
      )}
    >
      <Avatar className="h-9 w-9">
        <AvatarImage src={user.avatarUrl} alt={user.name} />
        <AvatarFallback>{user.name.charAt(0)}</AvatarFallback>
      </Avatar>
      <div
        className={cn(
          'flex flex-col gap-1 rounded-lg p-3 max-w-xs sm:max-w-md',
          isCurrentUser
            ? 'bg-primary text-primary-foreground'
            : 'bg-card'
        )}
      >
        <div className="flex items-center justify-between">
          <p className="font-semibold text-sm">{user.name}</p>
        </div>
        <p className="text-sm">{message}</p>
        <p className={cn("text-xs opacity-70", isCurrentUser ? "text-primary-foreground" : "text-muted-foreground")}>{timeAgo}</p>
      </div>
    </div>
  );
}
