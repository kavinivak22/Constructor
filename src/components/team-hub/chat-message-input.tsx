
'use client';

import { useState, useEffect } from 'react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Send } from 'lucide-react';
import { useSupabase } from '@/supabase/provider';
import { useToast } from '@/hooks/use-toast';
import type { Project } from '@/lib/data';

interface ChatMessageInputProps {
  projectId: string;
}

export function ChatMessageInput({ projectId }: ChatMessageInputProps) {
  const [message, setMessage] = useState('');
  const { supabase, user } = useSupabase();
  const { toast } = useToast();
  const [project, setProject] = useState<Project | null>(null);

  useEffect(() => {
    const fetchProject = async () => {
      if (!projectId) return;
      const { data, error } = await supabase
        .from('projects')
        .select('id, name')
        .eq('id', projectId)
        .single();

      if (error) {
        console.error('Error fetching project:', error);
      } else {
        setProject(data as Project);
      }
    };

    fetchProject();
  }, [supabase, projectId]);

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (message.trim() === '' || !user || !project) return;

    try {
      // 1. Add the new message to the chat
      const { error: messageError } = await supabase.from('chat_messages').insert([{
        project_id: projectId,
        user_id: user.id,
        message: message,
      }]);

      if (messageError) throw messageError;

      // 2. Create notifications for all other users in the project
      const { data: projectMembers } = await supabase
        .from('users')
        .select('id')
        .contains('projectIds', [projectId]);

      const otherUsers = projectMembers
        ? projectMembers.map(u => u.id).filter(id => id !== user.id)
        : [];

      if (otherUsers.length > 0) {
        const notifications = otherUsers.map(otherUserId => ({
          user_id: otherUserId,
          title: `New message in ${project.name}`,
          description: `${user.user_metadata.full_name || user.email}: "${message.substring(0, 50)}${message.length > 50 ? '...' : ''}"`,
          read: false,
          link: `/team-hub?projectId=${projectId}`,
          category: 'team',
          icon: 'MessageSquare',
          project_id: projectId,
        }));

        const { error: notificationError } = await supabase.from('notifications').insert(notifications);
        if (notificationError) throw notificationError;
      }

      setMessage('');

    } catch (error: any) {
      toast({
        title: 'Error sending message',
        description: error.message,
        variant: 'destructive',
      });
    }
  };

  return (
    <form onSubmit={handleSendMessage} className="flex w-full items-center gap-2">
      <Input
        type="text"
        placeholder="Type a message..."
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        className="flex-1"
        disabled={!user || !project}
      />
      <Button type="submit" size="icon" disabled={!user || !project || message.trim() === ''}>
        <Send className="h-4 w-4" />
        <span className="sr-only">Send</span>
      </Button>
    </form>
  );
}
