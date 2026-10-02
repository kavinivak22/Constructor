'use client';

import { useState, useEffect, useMemo, useRef, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { useSupabase } from '@/supabase/provider';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Checkbox } from '@/components/ui/checkbox';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { useToast } from '@/hooks/use-toast';
import {
  Loader2,
  Upload,
  FileText,
  Trash2,
  Download,
  File as FileIcon,
  Image as ImageIcon,
  ArrowLeft,
  StickyNote,
  Pin,
  Copy,
  Plus,
  Eye,
  ExternalLink,
  X,
  Pencil,
  Check,
  Search,
  ChevronDown,
} from 'lucide-react';
import { format } from 'date-fns';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  DropdownMenuSeparator,
} from '@/components/ui/dropdown-menu';
import { Document, Project } from '@/lib/data';
import { UploadProgressPopup } from '@/components/upload-progress-popup';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Label } from '@/components/ui/label';
import { DocumentPreviewDialog } from '@/components/document-preview-dialog';

const NOTE_CATEGORIES = [
  'General',
  'Site Utilities',
  'Engineering Specs',
  'Access & Codes',
  'Client Memos',
  'Site Measurements',
  'Other',
];

interface ParsedNote {
  id: string;
  title: string;
  content: string;
  category: string;
  pinned: boolean;
  createdAt: string;
  author: string;
  uploaderId: string;
  rawDoc: Document;
}

function parseNoteData(doc: Document): ParsedNote {
  let content = doc.url || '';
  let pinned = false;
  try {
    if (doc.url && (doc.url.startsWith('{') || doc.url.startsWith('['))) {
      const parsed = JSON.parse(doc.url);
      if (parsed && typeof parsed === 'object') {
        content = parsed.content ?? doc.url;
        pinned = Boolean(parsed.pinned);
      }
    }
  } catch {
    content = doc.url || '';
  }

  return {
    id: doc.id,
    title: doc.name,
    content,
    category: doc.category || 'General',
    pinned,
    createdAt: doc.createdAt,
    author: doc.uploader?.name || 'Unknown',
    uploaderId: doc.uploaderId,
    rawDoc: doc,
  };
}

function ProjectPouchContent() {
  const router = useRouter();
  const { supabase, user } = useSupabase();
  const searchParams = useSearchParams();
  const urlProjectId = searchParams.get('projectId');
  const { toast } = useToast();

  const handleBack = () => {
    if (urlProjectId) {
      router.push(`/projects/${urlProjectId}`);
    } else if (typeof window !== 'undefined' && window.history.length > 1) {
      router.back();
    } else {
      router.push('/projects');
    }
  };

  const [activeTab, setActiveTab] = useState<'documents' | 'notes'>('documents');
  const [documents, setDocuments] = useState<Document[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProject, setSelectedProject] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const currentProject = useMemo(() => {
    return projects.find((p) => p.id === selectedProject);
  }, [projects, selectedProject]);

  // Document filters & edit
  const [selectedCategory, setSelectedCategory] = useState<string>('All');
  const [editingDoc, setEditingDoc] = useState<Document | null>(null);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [editName, setEditName] = useState('');
  const [editCategory, setEditCategory] = useState('');

  // Upload Progress State
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadStatus, setUploadStatus] = useState<'uploading' | 'completed' | 'error'>('uploading');
  const [uploadFileName, setUploadFileName] = useState('');
  const [showProgressPopup, setShowProgressPopup] = useState(false);
  const [uploadErrorMsg, setUploadErrorMsg] = useState<string | undefined>(undefined);

  // Preview State (Documents)
  const [previewDoc, setPreviewDoc] = useState<Document | null>(null);
  const [isPreviewOpen, setIsPreviewOpen] = useState(false);

  // Search & Filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedNoteCategory, setSelectedNoteCategory] = useState<string>('All');
  const [isCreateNoteOpen, setIsCreateNoteOpen] = useState(false);
  const [newNoteTitle, setNewNoteTitle] = useState('');
  const [newNoteCategory, setNewNoteCategory] = useState('General');
  const [newNoteContent, setNewNoteContent] = useState('');
  const [newNotePinned, setNewNotePinned] = useState(false);
  const [isSavingNote, setIsSavingNote] = useState(false);

  const [viewingNote, setViewingNote] = useState<ParsedNote | null>(null);
  const [isViewNoteOpen, setIsViewNoteOpen] = useState(false);

  const [isEditNoteOpen, setIsEditNoteOpen] = useState(false);
  const [editingNoteId, setEditingNoteId] = useState('');
  const [editNoteTitle, setEditNoteTitle] = useState('');
  const [editNoteCategory, setEditNoteCategory] = useState('General');
  const [editNoteContent, setEditNoteContent] = useState('');
  const [editNotePinned, setEditNotePinned] = useState(false);

  const [copiedNoteId, setCopiedNoteId] = useState<string | null>(null);

  useEffect(() => {
    if (user) {
      fetchProjects();
    }
  }, [user]);

  useEffect(() => {
    if (urlProjectId && projects.some((p) => p.id === urlProjectId)) {
      setSelectedProject(urlProjectId);
    }
  }, [urlProjectId, projects]);

  useEffect(() => {
    if (selectedProject) {
      fetchDocuments(selectedProject);
    } else {
      setDocuments([]);
    }
  }, [selectedProject]);

  const fetchProjects = async () => {
    try {
      const { data: membersData, error: membersError } = await supabase
        .from('project_members')
        .select('project_id')
        .eq('user_id', user?.id);

      if (membersError) throw membersError;

      const projectIds = membersData?.map((m) => m.project_id) || [];

      if (projectIds.length > 0) {
        const { data: projectsData, error: projectsError } = await supabase
          .from('projects')
          .select('*')
          .in('id', projectIds);

        if (projectsError) throw projectsError;
        setProjects(projectsData || []);
        if (projectsData && projectsData.length > 0) {
          const match = urlProjectId && projectsData.some((p) => p.id === urlProjectId);
          setSelectedProject(match ? (urlProjectId as string) : projectsData[0].id);
        }
      }
    } catch (error: any) {
      console.error('Error fetching projects:', error);
      toast({
        title: 'Error',
        description: 'Failed to load projects.',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const fetchDocuments = async (projectId: string) => {
    setLoading(true);
    try {
      const { data, error } = await supabase
        .from('documents')
        .select('*, uploader:users(displayName:display_name, photoURL:photo_url)')
        .eq('project_id', projectId)
        .order('created_at', { ascending: false });

      if (error) throw error;

      // Fetch read status for current user
      const { data: readData } = await supabase
        .from('document_reads')
        .select('document_id')
        .eq('user_id', user?.id);

      const readDocIds = new Set((readData || []).map((r: any) => r.document_id));

      const mappedDocs: Document[] = (data || []).map((doc: any) => ({
        id: doc.id,
        projectId: doc.project_id,
        uploaderId: doc.uploader_id,
        name: doc.name,
        url: doc.url,
        size: doc.size,
        type: doc.type,
        category: doc.category || 'General',
        createdAt: doc.created_at,
        isRead: readDocIds.has(doc.id),
        uploader: {
          name: doc.uploader?.displayName || 'Unknown',
          photoURL: doc.uploader?.photoURL,
        },
      }));

      setDocuments(mappedDocs);
    } catch (error: any) {
      console.error('Error fetching documents:', error);
      toast({
        title: 'Error',
        description: `Failed to load documents: ${error.message}`,
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  // Separation of Documents vs Notes with Search & Filter
  const fileDocuments = useMemo(() => documents.filter((d) => d.type !== 'note'), [documents]);
  const noteDocuments = useMemo(() => documents.filter((d) => d.type === 'note'), [documents]);

  const filteredFileDocs = useMemo(() => {
    const query = searchQuery.toLowerCase().trim();
    return fileDocuments.filter((doc) => {
      const matchesCategory = selectedCategory === 'All' || doc.category === selectedCategory;
      const matchesSearch = !query || doc.name.toLowerCase().includes(query);
      return matchesCategory && matchesSearch;
    });
  }, [fileDocuments, selectedCategory, searchQuery]);

  const filteredNoteDocs = useMemo(() => {
    const query = searchQuery.toLowerCase().trim();
    return noteDocuments
      .map(parseNoteData)
      .filter((n) => {
        const matchesCategory = selectedNoteCategory === 'All' || n.category === selectedNoteCategory;
        const matchesSearch =
          !query || n.title.toLowerCase().includes(query) || n.content.toLowerCase().includes(query);
        return matchesCategory && matchesSearch;
      })
      .sort((a, b) => {
        if (a.pinned && !b.pinned) return -1;
        if (!a.pinned && b.pinned) return 1;
        const dateA = new Date(
          typeof a.createdAt === 'string' ? a.createdAt : (a.createdAt as any)?.seconds * 1000
        ).getTime();
        const dateB = new Date(
          typeof b.createdAt === 'string' ? b.createdAt : (b.createdAt as any)?.seconds * 1000
        ).getTime();
        return dateB - dateA;
      });
  }, [noteDocuments, selectedNoteCategory, searchQuery]);

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    if (!event.target.files || event.target.files.length === 0) return;
    if (!selectedProject) {
      toast({
        title: 'No Project Selected',
        description: 'Please select a project to upload documents to.',
        variant: 'destructive',
      });
      return;
    }

    const file = event.target.files[0];

    // 5MB Limit
    if (file.size > 5 * 1024 * 1024) {
      toast({
        title: 'File too large',
        description: 'Document must be less than 5MB.',
        variant: 'destructive',
      });
      return;
    }

    if (!user) {
      toast({
        title: 'Authentication required',
        description: 'Please sign in to upload documents.',
        variant: 'destructive',
      });
      return;
    }

    setUploading(true);
    setUploadProgress(0);
    setUploadStatus('uploading');
    setUploadFileName(file.name);
    setUploadErrorMsg(undefined);
    setShowProgressPopup(true);

    const progressInterval = setInterval(() => {
      setUploadProgress((prev) => {
        if (prev >= 90) return prev;
        return prev + 10;
      });
    }, 200);

    try {
      const fileExt = file.name.split('.').pop() || 'bin';
      const cleanFileName = file.name.replace(/[^a-zA-Z0-9.-]/g, '_');
      // Using user.id as first folder segment ensures compatibility with Supabase's auth.uid() folder RLS policy
      const fileName = `${user.id}/${selectedProject}/${Date.now()}_${cleanFileName}`;

      const { error: uploadError } = await supabase.storage
        .from('project-documents')
        .upload(fileName, file);

      if (uploadError) throw uploadError;

      const {
        data: { publicUrl },
      } = supabase.storage.from('project-documents').getPublicUrl(fileName);

      const { error: dbError } = await supabase.from('documents').insert([
        {
          project_id: selectedProject,
          uploader_id: user.id,
          name: file.name,
          url: publicUrl,
          size: file.size,
          type: file.type || 'application/octet-stream',
          category: selectedCategory === 'All' ? 'General' : selectedCategory,
        },
      ]);

      if (dbError) throw dbError;

      clearInterval(progressInterval);
      setUploadProgress(100);
      setUploadStatus('completed');

      toast({
        title: 'Success',
        description: 'Document uploaded successfully.',
      });

      fetchDocuments(selectedProject);
    } catch (error: any) {
      console.error('Error uploading document:', error);
      clearInterval(progressInterval);
      setUploadStatus('error');
      setUploadErrorMsg(error.message);

      toast({
        title: 'Upload Failed',
        description: error.message?.includes('row-level security')
          ? 'Storage permission error: Please ensure the project-documents storage policy is configured in Supabase.'
          : error.message,
        variant: 'destructive',
      });
    } finally {
      setUploading(false);
      event.target.value = '';
    }
  };

  const deleteDocument = async (doc: Document) => {
    try {
      const { error } = await supabase.from('documents').delete().eq('id', doc.id);

      if (error) throw error;

      if (doc.url && doc.type !== 'note') {
        const urlParts = doc.url.split('/project-documents/');
        const storagePath = urlParts[1];
        if (storagePath) {
          await supabase.storage
            .from('project-documents')
            .remove([decodeURIComponent(storagePath)])
            .catch(() => {});
        }
      }

      setDocuments((prev) => prev.filter((d) => d.id !== doc.id));
      if (viewingNote && viewingNote.id === doc.id) {
        setIsViewNoteOpen(false);
        setViewingNote(null);
      }
      toast({
        title: 'Deleted',
        description: `${doc.type === 'note' ? 'Note' : 'Document'} removed successfully.`,
      });
    } catch (error: any) {
      toast({
        title: 'Error',
        description: `Failed to delete ${doc.type === 'note' ? 'note' : 'document'}.`,
        variant: 'destructive',
      });
    }
  };

  const handleEdit = (doc: Document) => {
    setEditingDoc(doc);
    setEditName(doc.name);
    setEditCategory(doc.category || 'General');
    setIsEditOpen(true);
  };

  const saveEdit = async () => {
    if (!editingDoc) return;
    try {
      const { error } = await supabase
        .from('documents')
        .update({ name: editName, category: editCategory })
        .eq('id', editingDoc.id);

      if (error) throw error;

      setDocuments((prev) =>
        prev.map((d) => (d.id === editingDoc.id ? { ...d, name: editName, category: editCategory } : d))
      );

      setIsEditOpen(false);
      toast({
        title: 'Success',
        description: 'Document updated successfully.',
      });
    } catch (error: any) {
      toast({
        title: 'Error',
        description: 'Failed to update document.',
        variant: 'destructive',
      });
    }
  };

  // Notes Operations
  const handleCreateNote = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newNoteTitle.trim()) {
      toast({
        title: 'Title required',
        description: 'Please enter a title for your note.',
        variant: 'destructive',
      });
      return;
    }
    if (!selectedProject) {
      toast({
        title: 'No Project Selected',
        description: 'Please select a project first.',
        variant: 'destructive',
      });
      return;
    }

    setIsSavingNote(true);
    try {
      const payload = JSON.stringify({
        content: newNoteContent.trim(),
        pinned: newNotePinned,
      });

      const { data, error } = await supabase
        .from('documents')
        .insert([
          {
            project_id: selectedProject,
            uploader_id: user?.id,
            name: newNoteTitle.trim(),
            url: payload,
            size: newNoteContent.length,
            type: 'note',
            category: newNoteCategory || 'General',
          },
        ])
        .select('*, uploader:users(displayName:display_name, photoURL:photo_url)')
        .single();

      if (error) throw error;

      if (data) {
        const mappedNote: Document = {
          id: data.id,
          projectId: data.project_id,
          uploaderId: data.uploader_id,
          name: data.name,
          url: data.url,
          size: data.size,
          type: data.type,
          category: data.category || 'General',
          createdAt: data.created_at,
          isRead: true,
          uploader: {
            name: data.uploader?.displayName || 'Unknown',
            photoURL: data.uploader?.photoURL,
          },
        };
        setDocuments((prev) => [mappedNote, ...prev]);
      } else {
        fetchDocuments(selectedProject);
      }

      setNewNoteTitle('');
      setNewNoteCategory('General');
      setNewNoteContent('');
      setNewNotePinned(false);
      setIsCreateNoteOpen(false);

      toast({
        title: 'Note Saved',
        description: 'Your project note has been recorded.',
      });
    } catch (error: any) {
      console.error('Error creating note:', error);
      toast({
        title: 'Failed to create note',
        description: error.message || 'An error occurred while saving the note.',
        variant: 'destructive',
      });
    } finally {
      setIsSavingNote(false);
    }
  };

  const openEditNote = (note: ParsedNote, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    setEditingNoteId(note.id);
    setEditNoteTitle(note.title);
    setEditNoteCategory(note.category);
    setEditNoteContent(note.content);
    setEditNotePinned(note.pinned);
    setIsEditNoteOpen(true);
  };

  const handleUpdateNote = async () => {
    if (!editingNoteId || !editNoteTitle.trim()) return;

    setIsSavingNote(true);
    try {
      const payload = JSON.stringify({
        content: editNoteContent.trim(),
        pinned: editNotePinned,
      });

      const { error } = await supabase
        .from('documents')
        .update({
          name: editNoteTitle.trim(),
          category: editNoteCategory,
          url: payload,
          size: editNoteContent.length,
        })
        .eq('id', editingNoteId);

      if (error) throw error;

      setDocuments((prev) =>
        prev.map((d) =>
          d.id === editingNoteId
            ? {
                ...d,
                name: editNoteTitle.trim(),
                category: editNoteCategory,
                url: payload,
                size: editNoteContent.length,
              }
            : d
        )
      );

      setIsEditNoteOpen(false);

      if (viewingNote && viewingNote.id === editingNoteId) {
        setViewingNote({
          ...viewingNote,
          title: editNoteTitle.trim(),
          category: editNoteCategory,
          content: editNoteContent.trim(),
          pinned: editNotePinned,
        });
      }

      toast({
        title: 'Note Updated',
        description: 'Your changes have been saved.',
      });
    } catch (error: any) {
      console.error('Error updating note:', error);
      toast({
        title: 'Update Failed',
        description: error.message || 'Could not update note.',
        variant: 'destructive',
      });
    } finally {
      setIsSavingNote(false);
    }
  };

  const handleTogglePin = async (note: ParsedNote, e: React.MouseEvent) => {
    e.stopPropagation();
    const updatedPinned = !note.pinned;
    const payload = JSON.stringify({
      content: note.content,
      pinned: updatedPinned,
    });

    setDocuments((prev) => prev.map((d) => (d.id === note.id ? { ...d, url: payload } : d)));

    try {
      const { error } = await supabase.from('documents').update({ url: payload }).eq('id', note.id);

      if (error) throw error;

      toast({
        title: updatedPinned ? 'Note Pinned' : 'Note Unpinned',
        description: updatedPinned ? 'Note pinned to top of the list.' : 'Note unpinned.',
      });
    } catch (error: any) {
      console.error('Error updating pin:', error);
      setDocuments((prev) => prev.map((d) => (d.id === note.id ? { ...d, url: note.rawDoc.url } : d)));
      toast({
        title: 'Error',
        description: 'Failed to update pin state.',
        variant: 'destructive',
      });
    }
  };

  const handleCopyNote = (text: string, noteId: string, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    if (navigator?.clipboard?.writeText) {
      navigator.clipboard.writeText(text);
      setCopiedNoteId(noteId);
      setTimeout(() => {
        setCopiedNoteId((prev) => (prev === noteId ? null : prev));
      }, 2000);
      toast({
        title: 'Copied to clipboard',
        description: 'Note text copied successfully.',
      });
    }
  };

  const markAsRead = async (docId: string) => {
    try {
      await supabase.from('document_reads').insert([{ document_id: docId, user_id: user?.id }]).select();
      setDocuments((prev) => prev.map((d) => (d.id === docId ? { ...d, isRead: true } : d)));
    } catch {
      // Ignore unique constraint violation
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const getFileIcon = (type: string) => {
    if (type.startsWith('image/')) return <ImageIcon className="h-8 w-8 text-blue-500" />;
    if (type.includes('pdf')) return <FileText className="h-8 w-8 text-red-500" />;
    return <FileIcon className="h-8 w-8 text-gray-500" />;
  };

  const handlePreview = (doc: Document) => {
    setPreviewDoc(doc);
    setIsPreviewOpen(true);
    if (!doc.isRead) {
      markAsRead(doc.id);
    }
  };

  return (
    <div className="flex flex-col h-full bg-transparent p-4 md:p-6 space-y-5">
      <UploadProgressPopup
        progress={uploadProgress}
        fileName={uploadFileName}
        isVisible={showProgressPopup}
        status={uploadStatus}
        errorMessage={uploadErrorMsg}
        onClose={() => setShowProgressPopup(false)}
      />

      {/* Header Row: Title, Back button, Project selector/badge & Universal Action button */}
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2 sm:gap-2.5 min-w-0">
          <Button
            variant="ghost"
            size="icon"
            onClick={handleBack}
            className="h-8 w-8 sm:h-9 sm:w-9 shrink-0 rounded-xl hover:bg-muted"
            title="Back"
          >
            <ArrowLeft className="h-4 w-4 sm:h-5 sm:w-5" />
          </Button>
          <div className="flex items-center gap-2 min-w-0">
            <h1 className="text-lg sm:text-2xl font-bold tracking-tight font-headline whitespace-nowrap shrink-0">
              Project Pouch
            </h1>
            {urlProjectId && currentProject && (
              <Badge
                variant="outline"
                className="text-xs font-semibold px-2 py-0.5 rounded-lg border-border/60 bg-muted/40 max-w-[100px] sm:max-w-[200px] truncate shrink"
                title={currentProject.name}
              >
                {currentProject.name}
              </Badge>
            )}
          </div>
        </div>

        {/* Right side: Desktop Project Selector (if no projectId query param) & Universal Upload Action */}
        <div className="flex items-center gap-2 shrink-0">
          {!urlProjectId && projects.length > 0 && (
            <div className="hidden sm:block">
              <Select value={selectedProject} onValueChange={setSelectedProject}>
                <SelectTrigger className="w-[180px] md:w-[220px] h-9 text-xs sm:text-sm bg-background border-border/60 rounded-xl shadow-2xs">
                  <SelectValue placeholder="Select Project" />
                </SelectTrigger>
                <SelectContent>
                  {projects.map((project) => (
                    <SelectItem key={project.id} value={project.id}>
                      {project.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          {/* Universal Upload / Add Action Dropdown */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="default"
                size="sm"
                disabled={uploading || !selectedProject}
                className="h-9 px-2.5 rounded-xl shadow-xs gap-1 shrink-0"
                title="Upload Document or Create Note"
                aria-label="Upload Document or Create Note"
              >
                {uploading ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Upload className="h-4 w-4" />
                )}
                <ChevronDown className="h-3.5 w-3.5 opacity-70" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56 p-1.5 rounded-xl shadow-2xl border border-border bg-popover text-popover-foreground">
              <DropdownMenuItem
                onClick={() => fileInputRef.current?.click()}
                disabled={uploading || !selectedProject}
                className="flex items-start gap-2.5 p-2 rounded-lg cursor-pointer hover:bg-muted focus:bg-muted"
              >
                <div className="p-1.5 rounded-md bg-primary/10 text-primary shrink-0 mt-0.5">
                  <Upload className="h-4 w-4" />
                </div>
                <div className="flex flex-col min-w-0">
                  <span className="text-xs sm:text-sm font-semibold text-foreground">Upload Documents</span>
                  <span className="text-[11px] text-muted-foreground truncate">PDF, Blueprint, Images</span>
                </div>
              </DropdownMenuItem>

              <DropdownMenuSeparator className="my-1 bg-border" />

              <DropdownMenuItem
                onClick={() => setIsCreateNoteOpen(true)}
                disabled={!selectedProject}
                className="flex items-start gap-2.5 p-2 rounded-lg cursor-pointer hover:bg-muted focus:bg-muted"
              >
                <div className="p-1.5 rounded-md bg-amber-500/10 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5">
                  <StickyNote className="h-4 w-4" />
                </div>
                <div className="flex flex-col min-w-0">
                  <span className="text-xs sm:text-sm font-semibold text-foreground">Create New Note</span>
                  <span className="text-[11px] text-muted-foreground truncate">Specs, passcodes, memos</span>
                </div>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>

          {/* Hidden File Input for Universal Upload */}
          <input
            type="file"
            ref={fileInputRef}
            className="hidden"
            onChange={handleFileUpload}
            disabled={uploading || !selectedProject}
          />
        </div>
      </div>

      {/* Mobile Project Selector: Full-width dedicated row so it never squashes the header title */}
      {!urlProjectId && projects.length > 0 && (
        <div className="sm:hidden w-full">
          <Select value={selectedProject} onValueChange={setSelectedProject}>
            <SelectTrigger className="w-full h-9 text-xs bg-background border-border/60 rounded-xl shadow-2xs">
              <SelectValue placeholder="Select Project" />
            </SelectTrigger>
            <SelectContent>
              {projects.map((project) => (
                <SelectItem key={project.id} value={project.id}>
                  {project.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      )}

      {!selectedProject ? (
        <Card className="glass-card flex flex-col items-center justify-center h-[400px] text-center p-6">
          <div className="bg-primary/10 p-4 rounded-full mb-4">
            <FileText className="h-12 w-12 text-primary" />
          </div>
          <h3 className="text-lg font-semibold">No Project Selected</h3>
          <p className="text-muted-foreground max-w-sm mt-2">
            Please select a project from the dropdown above to view its documents and notes.
          </p>
        </Card>
      ) : (
        <Tabs
          value={activeTab}
          onValueChange={(val) => setActiveTab(val as 'documents' | 'notes')}
          className="space-y-4"
        >
          {/* Subheader Toolbar: Segmented Tabs & Single-row Search + Category Filter */}
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2.5 border-b border-border/40 pb-3">
            {/* Segmented Control Tabs */}
            <TabsList className="grid grid-cols-2 w-full sm:w-[270px] h-9 p-0.5 bg-muted/70 rounded-xl shrink-0">
              <TabsTrigger
                value="documents"
                className="flex items-center justify-center gap-1.5 rounded-lg text-xs font-medium h-8 data-[state=active]:bg-background data-[state=active]:shadow-2xs"
              >
                <FileText className="h-3.5 w-3.5" />
                <span>Documents</span>
                <span className="text-[10px] px-1.5 py-0.2 rounded-full bg-muted/80 text-muted-foreground font-semibold">
                  {fileDocuments.length}
                </span>
              </TabsTrigger>
              <TabsTrigger
                value="notes"
                className="flex items-center justify-center gap-1.5 rounded-lg text-xs font-medium h-8 data-[state=active]:bg-background data-[state=active]:shadow-2xs"
              >
                <StickyNote className="h-3.5 w-3.5" />
                <span>Notes</span>
                <span className="text-[10px] px-1.5 py-0.2 rounded-full bg-muted/80 text-muted-foreground font-semibold">
                  {noteDocuments.length}
                </span>
              </TabsTrigger>
            </TabsList>

            {/* Single Row: Search Bar & Category Filter */}
            <div className="flex items-center gap-2 w-full sm:w-auto sm:flex-1 sm:max-w-[480px]">
              <div className="relative flex-1 min-w-0">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground pointer-events-none" />
                <Input
                  type="text"
                  placeholder={activeTab === 'documents' ? 'Search documents...' : 'Search notes & specs...'}
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-8 pr-7 h-9 text-xs sm:text-sm bg-background border-border/60 rounded-xl shadow-2xs focus-visible:ring-1 w-full"
                />
                {searchQuery && (
                  <button
                    onClick={() => setSearchQuery('')}
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground p-0.5 rounded-full"
                    title="Clear search"
                  >
                    <X className="h-3 w-3" />
                  </button>
                )}
              </div>

              {activeTab === 'documents' ? (
                <Select value={selectedCategory} onValueChange={setSelectedCategory}>
                  <SelectTrigger className="w-[125px] sm:w-[155px] shrink-0 h-9 text-xs sm:text-sm bg-background border-border/60 rounded-xl shadow-2xs">
                    <SelectValue placeholder="Category" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="All">All Categories</SelectItem>
                    <SelectItem value="General">General</SelectItem>
                    <SelectItem value="Contracts">Contracts</SelectItem>
                    <SelectItem value="Blueprints">Blueprints</SelectItem>
                    <SelectItem value="Invoices">Invoices</SelectItem>
                    <SelectItem value="Reports">Reports</SelectItem>
                  </SelectContent>
                </Select>
              ) : (
                <Select value={selectedNoteCategory} onValueChange={setSelectedNoteCategory}>
                  <SelectTrigger className="w-[125px] sm:w-[155px] shrink-0 h-9 text-xs sm:text-sm bg-background border-border/60 rounded-xl shadow-2xs">
                    <SelectValue placeholder="Category" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="All">All Categories</SelectItem>
                    {NOTE_CATEGORIES.map((cat) => (
                      <SelectItem key={cat} value={cat}>
                        {cat}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            </div>
          </div>

          {/* Documents Tab Content */}
          <TabsContent value="documents" className="m-0 focus-visible:outline-none">
            {loading ? (
              <div className="flex justify-center py-16">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
              </div>
            ) : filteredFileDocs.length === 0 ? (
              <Card className="glass-card flex flex-col items-center justify-center py-14 text-center p-6">
                {searchQuery ? (
                  <>
                    <Search className="h-8 w-8 text-muted-foreground/60 mb-2" />
                    <p className="font-semibold text-sm">No documents matching &ldquo;{searchQuery}&rdquo;</p>
                    <p className="text-xs text-muted-foreground mt-1 mb-3">Try searching for a different keyword or category.</p>
                    <Button variant="outline" size="sm" onClick={() => setSearchQuery('')} className="rounded-xl text-xs h-8">
                      Clear Search
                    </Button>
                  </>
                ) : (
                  <>
                    <FileText className="h-10 w-10 text-muted-foreground/60 mb-2" />
                    <p className="font-medium text-foreground">No documents found for this project.</p>
                    <p className="text-xs sm:text-sm text-muted-foreground mt-1">
                      Upload a PDF, blueprint, or image to store it here.
                    </p>
                  </>
                )}
              </Card>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {filteredFileDocs.map((doc) => (
                    <Card
                      key={doc.id}
                      className="glass-card group relative overflow-hidden cursor-pointer transition-all hover:border-primary/40 hover:shadow-md"
                      onClick={() => handlePreview(doc)}
                    >
                      {!doc.isRead && (
                        <div className="absolute top-2 right-2 z-10">
                          <Badge className="bg-blue-500 hover:bg-blue-600 text-[10px] px-2 py-0.5">
                            NEW
                          </Badge>
                        </div>
                      )}
                      <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-2">
                        <div className="flex items-center gap-3">
                          {getFileIcon(doc.type)}
                          <div className="space-y-1 overflow-hidden">
                            <CardTitle
                              className="text-base font-medium truncate max-w-[170px]"
                              title={doc.name}
                            >
                              {doc.name}
                            </CardTitle>
                            <CardDescription className="text-xs flex items-center gap-2">
                              <Badge variant="outline" className="text-[10px] px-1 py-0 h-5">
                                {doc.category || 'General'}
                              </Badge>
                              <span>{formatFileSize(doc.size)}</span>
                            </CardDescription>
                          </div>
                        </div>
                      </CardHeader>
                      <CardContent>
                        <div className="flex items-center justify-between mt-4">
                          <div className="flex items-center gap-2 text-xs text-muted-foreground">
                            <span>
                              {format(
                                new Date(
                                  typeof doc.createdAt === 'string'
                                    ? doc.createdAt
                                    : (doc.createdAt as any).seconds * 1000
                                ),
                                'MMM d'
                              )}
                            </span>
                            <span>•</span>
                            <span>{doc.uploader?.name || 'Unknown'}</span>
                          </div>
                          <div className="flex gap-1">
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8 hover:text-primary"
                              asChild
                              onClick={(e) => e.stopPropagation()}
                              title="Download"
                            >
                              <a
                                href={`/api/documents/${doc.id}/content`}
                                target="_blank"
                                rel="noopener noreferrer"
                                download={doc.name}
                              >
                                <Download className="h-4 w-4" />
                              </a>
                            </Button>

                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8 hover:text-primary"
                              onClick={(e) => {
                                e.stopPropagation();
                                handleEdit(doc);
                              }}
                              title="Edit"
                            >
                              <Pencil className="h-4 w-4" />
                            </Button>

                            {(user?.id === doc.uploaderId || user?.role === 'admin') && (
                              <Button
                                variant="ghost"
                                size="icon"
                                className="h-8 w-8 text-destructive hover:text-destructive/90"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  deleteDocument(doc);
                                }}
                                title="Delete"
                              >
                                <Trash2 className="h-4 w-4" />
                              </Button>
                            )}
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  ))}
              </div>
            )}
          </TabsContent>

          {/* Notes Tab Content */}
          <TabsContent value="notes" className="m-0 focus-visible:outline-none">
            {loading ? (
              <div className="flex justify-center py-16">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
              </div>
            ) : filteredNoteDocs.length === 0 ? (
              <Card className="glass-card flex flex-col items-center justify-center py-14 text-center p-6">
                {searchQuery ? (
                  <>
                    <Search className="h-8 w-8 text-muted-foreground/60 mb-2" />
                    <p className="font-semibold text-sm">No notes matching &ldquo;{searchQuery}&rdquo;</p>
                    <p className="text-xs text-muted-foreground mt-1 mb-3">Try checking spelling or search a different keyword.</p>
                    <Button variant="outline" size="sm" onClick={() => setSearchQuery('')} className="rounded-xl text-xs h-8">
                      Clear Search
                    </Button>
                  </>
                ) : (
                  <>
                    <div className="bg-primary/10 p-3.5 rounded-full mb-3">
                      <StickyNote className="h-8 w-8 text-primary" />
                    </div>
                    <h3 className="font-semibold text-base sm:text-lg">No notes yet</h3>
                    <p className="text-xs sm:text-sm text-muted-foreground max-w-md mt-1 mb-4">
                      Keep critical site details handy: meter numbers, gate passcodes, mix ratios, bench marks, or client memos.
                    </p>
                    <Button
                      onClick={() => setIsCreateNoteOpen(true)}
                      size="sm"
                      className="rounded-xl gap-1.5 h-9"
                    >
                      <Plus className="h-4 w-4" />
                      Create First Note
                    </Button>
                  </>
                )}
              </Card>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {filteredNoteDocs.map((note) => {
                  const isCopied = copiedNoteId === note.id;
                  return (
                    <Card
                      key={note.id}
                      className={`glass-card group relative flex flex-col justify-between overflow-hidden cursor-pointer transition-all hover:border-primary/40 hover:shadow-md ${
                        note.pinned ? 'border-amber-500/40 bg-amber-500/[0.02] dark:bg-amber-500/[0.03]' : ''
                      }`}
                      onClick={() => {
                        setViewingNote(note);
                        setIsViewNoteOpen(true);
                      }}
                    >
                      <CardHeader className="p-4 pb-2 space-y-2">
                        {/* Badges & Pin Action */}
                        <div className="flex items-center justify-between gap-2">
                          <div className="flex items-center gap-1.5 flex-wrap">
                            <Badge
                              variant="outline"
                              className="text-[10px] px-1.5 py-0.2 font-medium border-border/80"
                            >
                              {note.category}
                            </Badge>
                            {note.pinned && (
                              <Badge className="bg-amber-500/15 text-amber-700 dark:text-amber-400 hover:bg-amber-500/25 border border-amber-500/30 text-[10px] px-1.5 py-0.2 gap-1 font-semibold">
                                <Pin className="h-2.5 w-2.5 fill-current" />
                                Pinned
                              </Badge>
                            )}
                          </div>

                          <Button
                            variant="ghost"
                            size="icon"
                            className={`h-7 w-7 rounded-lg ${
                              note.pinned
                                ? 'text-amber-600 dark:text-amber-400 hover:bg-amber-500/15'
                                : 'text-muted-foreground hover:text-foreground opacity-60 group-hover:opacity-100'
                            }`}
                            onClick={(e) => handleTogglePin(note, e)}
                            title={note.pinned ? 'Unpin note' : 'Pin note to top'}
                          >
                            <Pin className={`h-3.5 w-3.5 ${note.pinned ? 'fill-current' : ''}`} />
                          </Button>
                        </div>

                        {/* Title */}
                        <CardTitle
                          className="text-base font-semibold tracking-tight text-foreground line-clamp-1"
                          title={note.title}
                        >
                          {note.title}
                        </CardTitle>
                      </CardHeader>

                      <CardContent className="p-4 pt-1 flex-1 flex flex-col justify-between space-y-4">
                        {/* Note snippet */}
                        <div className="bg-muted/30 hover:bg-muted/50 transition-colors p-2.5 rounded-lg border border-border/40 text-xs sm:text-sm text-foreground/85 whitespace-pre-wrap font-sans line-clamp-4 leading-relaxed select-text">
                          {note.content || <span className="italic text-muted-foreground">No description provided</span>}
                        </div>

                        {/* Footer & Actions */}
                        <div className="flex items-center justify-between pt-1 border-t border-border/40 text-xs text-muted-foreground">
                          <div className="truncate max-w-[130px] sm:max-w-[150px]">
                            <span>
                              {format(
                                new Date(
                                  typeof note.createdAt === 'string'
                                    ? note.createdAt
                                    : (note.createdAt as any)?.seconds * 1000
                                ),
                                'MMM d'
                              )}
                            </span>
                            <span className="mx-1">•</span>
                            <span className="truncate">{note.author}</span>
                          </div>

                          <div className="flex items-center gap-1 shrink-0">
                            {/* Copy button */}
                            <Button
                              variant="ghost"
                              size="icon"
                              className={`h-7 w-7 rounded-lg transition-colors ${
                                isCopied ? 'text-emerald-500' : 'hover:text-primary'
                              }`}
                              onClick={(e) => handleCopyNote(note.content, note.id, e)}
                              title="Copy note content"
                            >
                              {isCopied ? <Check className="h-3.5 w-3.5" /> : <Copy className="h-3.5 w-3.5" />}
                            </Button>

                            {/* Edit button */}
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-7 w-7 rounded-lg hover:text-primary"
                              onClick={(e) => openEditNote(note, e)}
                              title="Edit note"
                            >
                              <Pencil className="h-3.5 w-3.5" />
                            </Button>

                            {/* Delete button (Author or Admin) */}
                            {(user?.id === note.uploaderId || user?.role === 'admin') && (
                              <Button
                                variant="ghost"
                                size="icon"
                                className="h-7 w-7 rounded-lg text-destructive hover:text-destructive/90"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  deleteDocument(note.rawDoc);
                                }}
                                title="Delete note"
                              >
                                <Trash2 className="h-3.5 w-3.5" />
                              </Button>
                            )}
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  );
                })}
              </div>
            )}
          </TabsContent>
        </Tabs>
      )}

      {/* Dialog: Create Note */}
      <Dialog open={isCreateNoteOpen} onOpenChange={setIsCreateNoteOpen}>
        <DialogContent className="sm:max-w-[520px] rounded-2xl">
          <form onSubmit={handleCreateNote}>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <StickyNote className="h-5 w-5 text-primary" />
                Add New Note
              </DialogTitle>
              <DialogDescription className="text-xs text-muted-foreground">
                Save site numbers, specs, passcodes, or quick instructions.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4 py-4">
              <div className="space-y-1.5">
                <Label htmlFor="new-note-title" className="text-xs font-semibold">
                  Note Title *
                </Label>
                <Input
                  id="new-note-title"
                  placeholder="e.g. EB Service Number, Mix Ratio, Gate Code"
                  value={newNoteTitle}
                  onChange={(e) => setNewNoteTitle(e.target.value)}
                  className="rounded-xl"
                  autoFocus
                  required
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="new-note-category" className="text-xs font-semibold">
                  Category
                </Label>
                <Select value={newNoteCategory} onValueChange={setNewNoteCategory}>
                  <SelectTrigger id="new-note-category" className="rounded-xl">
                    <SelectValue placeholder="Select Category" />
                  </SelectTrigger>
                  <SelectContent>
                    {NOTE_CATEGORIES.map((cat) => (
                      <SelectItem key={cat} value={cat}>
                        {cat}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="new-note-content" className="text-xs font-semibold">
                  Note Details / Content
                </Label>
                <Textarea
                  id="new-note-content"
                  placeholder="Type important numbers, specifications, instructions, or access info..."
                  value={newNoteContent}
                  onChange={(e) => setNewNoteContent(e.target.value)}
                  className="min-h-[140px] rounded-xl resize-y font-sans text-sm"
                />
              </div>

              <div className="flex items-center space-x-2 pt-1">
                <Checkbox
                  id="pin-note"
                  checked={newNotePinned}
                  onCheckedChange={(checked) => setNewNotePinned(Boolean(checked))}
                />
                <label
                  htmlFor="pin-note"
                  className="text-xs sm:text-sm font-medium leading-none cursor-pointer flex items-center gap-1.5"
                >
                  <Pin className="h-3.5 w-3.5 text-amber-500 fill-amber-500" />
                  Pin this note to the top
                </label>
              </div>
            </div>

            <DialogFooter className="gap-2 sm:gap-0">
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsCreateNoteOpen(false)}
                className="rounded-xl"
              >
                Cancel
              </Button>
              <Button type="submit" disabled={isSavingNote} className="rounded-xl gap-1.5">
                {isSavingNote && <Loader2 className="h-4 w-4 animate-spin" />}
                Save Note
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Dialog: Edit Note */}
      <Dialog open={isEditNoteOpen} onOpenChange={setIsEditNoteOpen}>
        <DialogContent className="sm:max-w-[520px] rounded-2xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Pencil className="h-4 w-4 text-primary" />
              Edit Note
            </DialogTitle>
            <DialogDescription className="text-xs text-muted-foreground">
              Update the title, category, or contents of this project note.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-4">
            <div className="space-y-1.5">
              <Label htmlFor="edit-note-title" className="text-xs font-semibold">
                Note Title *
              </Label>
              <Input
                id="edit-note-title"
                value={editNoteTitle}
                onChange={(e) => setEditNoteTitle(e.target.value)}
                className="rounded-xl"
                required
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="edit-note-category" className="text-xs font-semibold">
                Category
              </Label>
              <Select value={editNoteCategory} onValueChange={setEditNoteCategory}>
                <SelectTrigger id="edit-note-category" className="rounded-xl">
                  <SelectValue placeholder="Select Category" />
                </SelectTrigger>
                <SelectContent>
                  {NOTE_CATEGORIES.map((cat) => (
                    <SelectItem key={cat} value={cat}>
                      {cat}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="edit-note-content" className="text-xs font-semibold">
                Note Details
              </Label>
              <Textarea
                id="edit-note-content"
                value={editNoteContent}
                onChange={(e) => setEditNoteContent(e.target.value)}
                className="min-h-[140px] rounded-xl resize-y font-sans text-sm"
              />
            </div>

            <div className="flex items-center space-x-2 pt-1">
              <Checkbox
                id="edit-pin-note"
                checked={editNotePinned}
                onCheckedChange={(checked) => setEditNotePinned(Boolean(checked))}
              />
              <label
                htmlFor="edit-pin-note"
                className="text-xs sm:text-sm font-medium leading-none cursor-pointer flex items-center gap-1.5"
              >
                <Pin className="h-3.5 w-3.5 text-amber-500 fill-amber-500" />
                Pin this note to the top
              </label>
            </div>
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsEditNoteOpen(false)}
              className="rounded-xl"
            >
              Cancel
            </Button>
            <Button onClick={handleUpdateNote} disabled={isSavingNote} className="rounded-xl gap-1.5">
              {isSavingNote && <Loader2 className="h-4 w-4 animate-spin" />}
              Save Changes
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Dialog: View Note Full Modal */}
      <Dialog open={isViewNoteOpen} onOpenChange={setIsViewNoteOpen}>
        <DialogContent className="sm:max-w-[560px] rounded-2xl max-h-[85vh] flex flex-col">
          {viewingNote && (
            <>
              <DialogHeader className="space-y-2 border-b border-border/40 pb-3">
                <div className="flex items-center gap-2">
                  <Badge variant="outline" className="text-xs">
                    {viewingNote.category}
                  </Badge>
                  {viewingNote.pinned && (
                    <Badge className="bg-amber-500/15 text-amber-700 dark:text-amber-400 border border-amber-500/30 text-xs gap-1 font-semibold">
                      <Pin className="h-3 w-3 fill-current" />
                      Pinned
                    </Badge>
                  )}
                </div>
                <DialogTitle className="text-xl font-bold tracking-tight">
                  {viewingNote.title}
                </DialogTitle>
                <DialogDescription className="text-xs text-muted-foreground flex items-center gap-2">
                  <span>Created by {viewingNote.author}</span>
                  <span>•</span>
                  <span>
                    {format(
                      new Date(
                        typeof viewingNote.createdAt === 'string'
                          ? viewingNote.createdAt
                          : (viewingNote.createdAt as any)?.seconds * 1000
                      ),
                      'PPP'
                    )}
                  </span>
                </DialogDescription>
              </DialogHeader>

              <div className="flex-1 overflow-y-auto py-4">
                <div className="bg-muted/40 p-4 rounded-xl border border-border/50 text-sm whitespace-pre-wrap font-sans leading-relaxed select-text">
                  {viewingNote.content || <span className="italic text-muted-foreground">Empty note</span>}
                </div>
              </div>

              <DialogFooter className="flex-row items-center justify-between sm:justify-between border-t border-border/40 pt-3">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="rounded-xl gap-1.5 text-xs"
                  onClick={() => handleCopyNote(viewingNote.content, viewingNote.id)}
                >
                  {copiedNoteId === viewingNote.id ? (
                    <>
                      <Check className="h-3.5 w-3.5 text-emerald-500" />
                      Copied!
                    </>
                  ) : (
                    <>
                      <Copy className="h-3.5 w-3.5" />
                      Copy Content
                    </>
                  )}
                </Button>

                <div className="flex items-center gap-2">
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    className="rounded-xl gap-1.5 text-xs"
                    onClick={() => {
                      setIsViewNoteOpen(false);
                      openEditNote(viewingNote);
                    }}
                  >
                    <Pencil className="h-3.5 w-3.5" />
                    Edit
                  </Button>
                  <Button
                    type="button"
                    variant="default"
                    size="sm"
                    className="rounded-xl text-xs"
                    onClick={() => setIsViewNoteOpen(false)}
                  >
                    Done
                  </Button>
                </div>
              </DialogFooter>
            </>
          )}
        </DialogContent>
      </Dialog>

      {/* Standard Modern Document Preview Dialog */}
      <DocumentPreviewDialog
        isOpen={isPreviewOpen}
        onClose={() => {
          setIsPreviewOpen(false);
          setPreviewDoc(null);
        }}
        document={previewDoc}
        documents={fileDocuments}
        onSelectDocument={(doc) => {
          setPreviewDoc(doc as Document);
          if (!doc.isRead) {
            markAsRead(doc.id);
          }
        }}
      />

      {/* Edit Document Dialog */}
      <Dialog open={isEditOpen} onOpenChange={setIsEditOpen}>
        <DialogContent className="rounded-2xl">
          <DialogHeader>
            <DialogTitle>Edit Document</DialogTitle>
            <DialogDescription className="text-xs text-muted-foreground">
              Modify the document name or category.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="name">Document Name</Label>
              <Input
                id="name"
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                className="rounded-xl"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="category">Category</Label>
              <Select value={editCategory} onValueChange={setEditCategory}>
                <SelectTrigger className="rounded-xl">
                  <SelectValue placeholder="Select Category" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="General">General</SelectItem>
                  <SelectItem value="Contracts">Contracts</SelectItem>
                  <SelectItem value="Blueprints">Blueprints</SelectItem>
                  <SelectItem value="Invoices">Invoices</SelectItem>
                  <SelectItem value="Reports">Reports</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsEditOpen(false)} className="rounded-xl">
              Cancel
            </Button>
            <Button onClick={saveEdit} className="rounded-xl">
              Save Changes
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default function ProjectPouchPage() {
  return (
    <Suspense
      fallback={
        <div className="flex h-64 items-center justify-center">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        </div>
      }
    >
      <ProjectPouchContent />
    </Suspense>
  );
}
