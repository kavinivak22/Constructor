
'use client';

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useMemo, useState, useEffect, useRef } from 'react';
import { Project } from '@/lib/data';
import { Button } from '@/components/ui/button';
import Link from 'next/link';
import { PlusCircle } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card';
import { ChatMessage } from '@/components/team-hub/chat-message';
import { ChatMessageInput } from '@/components/team-hub/chat-message-input';
import { ScrollArea } from '@/components/ui/scroll-area';
import { ChatMessage as ChatMessageType } from '@/lib/data';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { useSearchParams } from 'next/navigation';
import { useSupabase } from '@/supabase/provider';


export default function TeamHubPage() {
  const { user } = useSupabase();
  const searchParams = useSearchParams();
  const projectIdFromUrl = searchParams.get('projectId');

  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(projectIdFromUrl);
  const scrollAreaRef = useRef<HTMLDivElement>(null);

  const projects: Project[] = [];
  const isLoadingProjects = false;

  const selectedProject = useMemo(() => {
    return projects?.find(p => p.id === selectedProjectId) ?? null;
  }, [projects, selectedProjectId]);

  const messages: ChatMessageType[] = [];
  const isLoadingMessages = false;

  const projectUsers: { id: string; displayName: string; photoURL: string }[] = [];

  const messagesWithUsers = useMemo(() => {
    if (!messages || !projectUsers) return [];
    return messages.map(message => {
      const messageUser = projectUsers.find(u => u.id === message.userId);
      return {
        ...message,
        user: {
          name: messageUser?.displayName || 'Unknown User',
          avatarUrl: messageUser?.photoURL || `https://avatar.vercel.sh/${message.userId}.png`,
        }
      };
    });
  }, [messages, projectUsers]);


  const handleProjectChange = (projectId: string) => {
    setSelectedProjectId(projectId);
  };

  useEffect(() => {
    // If there's a project ID from the URL, don't override it.
    if (projectIdFromUrl) return;

    if (projects && projects.length > 0 && !selectedProjectId) {
      setSelectedProjectId(projects[0].id);
    }
  }, [projects, selectedProjectId, projectIdFromUrl]);

  useEffect(() => {
    if (scrollAreaRef.current) {
      // Access the viewport element within the ScrollArea
      const viewport = scrollAreaRef.current.querySelector('div[data-radix-scroll-area-viewport]');
      if (viewport) {
        // A short delay can help ensure content is fully rendered
        setTimeout(() => {
          viewport.scrollTop = viewport.scrollHeight;
        }, 100);
      }
    }
  }, [messagesWithUsers]);

  return (
    <div className="flex flex-col h-full">
      <header className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 p-4 border-b md:px-6 shrink-0 bg-background sticky top-0 z-10">
        <div className="flex items-center gap-2">
          <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline">
            Team Communication Hub
          </h1>
        </div>
        <div className="flex flex-col md:flex-row items-stretch md:items-center gap-4 w-full md:w-auto">
          {isLoadingProjects ? (
            <Skeleton className="h-10 w-full md:w-48" />
          ) : (
            projects && projects.length > 0 && (
              <Select onValueChange={handleProjectChange} value={selectedProjectId ?? undefined}>
                <SelectTrigger className="w-full md:w-[180px]">
                  <SelectValue placeholder="Select a project" />
                </SelectTrigger>
                <SelectContent>
                  {projects.map(project => (
                    <SelectItem key={project.id} value={project.id}>
                      {project.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )
          )}
          <Link href="/projects/create" className="w-full md:w-auto">
            <Button className='w-full'>
              <PlusCircle className="mr-2 h-4 w-4" />
              Create Project
            </Button>
          </Link>
        </div>
      </header>
      <main className="flex-1 p-4 overflow-y-auto md:p-6">
        {!selectedProjectId && !isLoadingProjects && (
          <div className="flex flex-col items-center justify-center h-full text-center rounded-lg border-2 border-dashed bg-card/50">
            <h2 className="text-2xl font-bold font-headline">No Project Selected</h2>
            <p className="max-w-sm mt-2 text-muted-foreground">
              {projects && projects.length > 0 ? 'Please select a project to view its chat.' : 'Create a project to get started.'}
            </p>
            {(!projects || projects.length === 0) && (
              <Link href="/projects/create" className='mt-4'>
                <Button>
                  <PlusCircle className="mr-2 h-4 w-4" />
                  Create Project
                </Button>
              </Link>
            )}
          </div>
        )}
        {selectedProjectId && (
          <Card className="flex flex-col h-[calc(100vh-10rem)]">
            <CardHeader>
              <CardTitle>Team Chat for {selectedProject?.name}</CardTitle>
              <CardDescription>
                Real-time messaging with your project team.
              </CardDescription>
            </CardHeader>
            <CardContent className="flex-1 flex flex-col gap-4 overflow-hidden">
              <ScrollArea className="flex-1 pr-4" ref={scrollAreaRef}>
                <div className="space-y-4">
                  {isLoadingMessages && [...Array(5)].map((_, i) => <Skeleton key={i} className='h-12 w-full' />)}
                  {messagesWithUsers.map(msg => (
                    <ChatMessage
                      key={msg.id}
                      message={msg.message}
                      timestamp={new Date() as any}
                      user={msg.user}
                      isCurrentUser={false}
                    />
                  ))}
                </div>
              </ScrollArea>
            </CardContent>
            <CardFooter className="pt-4 border-t">
              <div className="flex items-start w-full gap-2">
                <Avatar className="h-9 w-9 hidden sm:flex">
                  <AvatarImage src={user?.user_metadata?.avatar_url} />
                  <AvatarFallback>{user?.user_metadata?.full_name?.charAt(0) || 'U'}</AvatarFallback>
                </Avatar>
                <ChatMessageInput projectId={selectedProjectId} />
              </div>
            </CardFooter>
          </Card>
        )}
      </main>
    </div>
  );
}
