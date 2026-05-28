'use client';

import { useState, useEffect, useTransition, useRef } from 'react';
import { useSupabase } from '@/supabase/provider';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/hooks/use-toast';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter
} from '@/components/ui/dialog';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { UploadProgressPopup } from '@/components/upload-progress-popup';
import { format } from 'date-fns';
import {
  Wallet,
  Plus,
  FileText,
  Trash2,
  Download,
  Eye,
  ExternalLink,
  X,
  Pencil,
  MapPin,
  Calendar,
  AlertTriangle,
  CheckCircle2,
  Circle,
  Loader2,
  Upload,
  Image as ImageIcon,
  File as FileIcon
} from 'lucide-react';

import { Document as PdfDocument, Page as PdfPage, pdfjs } from 'react-pdf';
import {
  getPersonalNotes,
  createPersonalNote,
  updatePersonalNote,
  deletePersonalNote,
  getPersonalDocuments,
  savePersonalDocumentMetadata,
  deletePersonalDocument,
  type PersonalNote,
  type PersonalDocument
} from './actions';

// Configure PDF worker
pdfjs.GlobalWorkerOptions.workerSrc = `//unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;

export default function PersonalPouchPage() {
  const { supabase, user } = useSupabase();
  const { toast } = useToast();

  const [notes, setNotes] = useState<PersonalNote[]>([]);
  const [documents, setDocuments] = useState<PersonalDocument[]>([]);
  const [loadingNotes, setLoadingNotes] = useState(true);
  const [loadingDocs, setLoadingDocs] = useState(true);
  const [isPending, startTransition] = useTransition();

  // Notes Form State
  const [isNoteDialogOpen, setIsNoteDialogOpen] = useState(false);
  const [editingNote, setEditingNote] = useState<PersonalNote | null>(null);
  const [noteTitle, setNoteTitle] = useState('');
  const [noteDescription, setNoteDescription] = useState('');
  const [notePriority, setNotePriority] = useState<'High' | 'Medium' | 'Low'>('Medium');
  const [noteLocation, setNoteLocation] = useState('');
  const [noteDueDate, setNoteDueDate] = useState('');

  // Documents State
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadStatus, setUploadStatus] = useState<'uploading' | 'completed' | 'error'>('uploading');
  const [uploadFileName, setUploadFileName] = useState('');
  const [showProgressPopup, setShowProgressPopup] = useState(false);
  const [uploadErrorMsg, setUploadErrorMsg] = useState<string | undefined>(undefined);
  const [selectedCategory, setSelectedCategory] = useState<string>('All');
  const [newDocCategory, setNewDocCategory] = useState<string>('General');

  // Preview State
  const [previewDoc, setPreviewDoc] = useState<PersonalDocument | null>(null);
  const [isPreviewOpen, setIsPreviewOpen] = useState(false);
  const [zoomLevel, setZoomLevel] = useState(1);
  const [panPosition, setPanPosition] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const [numPages, setNumPages] = useState<number | null>(null);
  const [pdfWidth, setPdfWidth] = useState<number>(600);

  function onDocumentLoadSuccess({ numPages }: { numPages: number }) {
    setNumPages(numPages);
  }

  useEffect(() => {
    const updateWidth = () => {
      setPdfWidth(window.innerWidth > 768 ? 600 : window.innerWidth * 0.9);
    };
    updateWidth();
    window.addEventListener('resize', updateWidth);
    return () => window.removeEventListener('resize', updateWidth);
  }, []);

  // Fetch initial data
  useEffect(() => {
    if (user) {
      fetchNotes();
      fetchDocuments();
    }
  }, [user]);

  const fetchNotes = async () => {
    setLoadingNotes(true);
    try {
      const data = await getPersonalNotes();
      setNotes(data);
    } catch (error: any) {
      console.error('Error fetching notes:', error);
      toast({
        title: 'Error',
        description: 'Failed to load personal notes.',
        variant: 'destructive',
      });
    } finally {
      setLoadingNotes(false);
    }
  };

  const fetchDocuments = async () => {
    setLoadingDocs(true);
    try {
      const data = await getPersonalDocuments();
      setDocuments(data);
    } catch (error: any) {
      console.error('Error fetching documents:', error);
      toast({
        title: 'Error',
        description: 'Failed to load personal documents.',
        variant: 'destructive',
      });
    } finally {
      setLoadingDocs(false);
    }
  };

  // Notes actions handlers
  const handleOpenNoteDialog = (note?: PersonalNote) => {
    if (note) {
      setEditingNote(note);
      setNoteTitle(note.title);
      setNoteDescription(note.description || '');
      setNotePriority(note.priority);
      setNoteLocation(note.location || '');
      setNoteDueDate(note.due_date ? note.due_date.split('T')[0] : '');
    } else {
      setEditingNote(null);
      setNoteTitle('');
      setNoteDescription('');
      setNotePriority('Medium');
      setNoteLocation('');
      setNoteDueDate('');
    }
    setIsNoteDialogOpen(true);
  };

  const handleSaveNote = () => {
    if (!noteTitle.trim()) {
      toast({
        title: 'Title required',
        description: 'Please specify a title for your note.',
        variant: 'destructive'
      });
      return;
    }

    startTransition(async () => {
      try {
        if (editingNote) {
          const updated = await updatePersonalNote(editingNote.id, {
            title: noteTitle,
            description: noteDescription || null,
            priority: notePriority,
            location: noteLocation || null,
            due_date: noteDueDate ? new Date(noteDueDate).toISOString() : null
          });
          setNotes(prev => prev.map(n => n.id === editingNote.id ? updated : n));
          toast({ title: 'Note updated', description: 'Your note was updated successfully.' });
        } else {
          const created = await createPersonalNote({
            title: noteTitle,
            description: noteDescription,
            priority: notePriority,
            location: noteLocation,
            due_date: noteDueDate ? new Date(noteDueDate).toISOString() : null
          });
          setNotes(prev => [created, ...prev]);
          toast({ title: 'Note created', description: 'Your note was created successfully.' });
        }
        setIsNoteDialogOpen(false);
      } catch (error: any) {
        toast({
          title: 'Error saving note',
          description: error.message,
          variant: 'destructive'
        });
      }
    });
  };

  const handleToggleComplete = async (note: PersonalNote) => {
    const updatedStatus = !note.is_completed;
    // Optimistic UI update
    setNotes(prev => prev.map(n => n.id === note.id ? { ...n, is_completed: updatedStatus } : n));

    try {
      await updatePersonalNote(note.id, { is_completed: updatedStatus });
    } catch (error) {
      // Revert on error
      setNotes(prev => prev.map(n => n.id === note.id ? { ...n, is_completed: !updatedStatus } : n));
      toast({
        title: 'Error',
        description: 'Failed to update note status.',
        variant: 'destructive'
      });
    }
  };

  const handleDeleteNote = async (id: string) => {
    // Optimistic update
    const previousNotes = [...notes];
    setNotes(prev => prev.filter(n => n.id !== id));

    try {
      await deletePersonalNote(id);
      toast({ title: 'Deleted', description: 'Note removed.' });
    } catch (error) {
      setNotes(previousNotes);
      toast({
        title: 'Error',
        description: 'Failed to delete note.',
        variant: 'destructive'
      });
    }
  };

  // File Upload Handlers
  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    if (!event.target.files || event.target.files.length === 0) return;
    const file = event.target.files[0];

    if (file.size > 5 * 1024 * 1024) {
      toast({
        title: 'File too large',
        description: 'File must be less than 5MB.',
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
      setUploadProgress((prev) => (prev >= 90 ? prev : prev + 10));
    }, 150);

    try {
      const fileExt = file.name.split('.').pop();
      const randomId = Math.random().toString(36).substring(2, 15);
      const fileName = `${user?.id}/${randomId}.${fileExt}`;

      // Upload to storage
      const { error: uploadError } = await supabase.storage
        .from('personal-documents')
        .upload(fileName, file);

      if (uploadError) throw uploadError;

      // Get URL (using public URL for DB storage, but secure proxy will check auth and access)
      const { data: { publicUrl } } = supabase.storage
        .from('personal-documents')
        .getPublicUrl(fileName);

      // Save metadata via action
      const newDoc = await savePersonalDocumentMetadata({
        name: file.name,
        url: publicUrl,
        size: file.size,
        type: file.type,
        category: newDocCategory
      });

      clearInterval(progressInterval);
      setUploadProgress(100);
      setUploadStatus('completed');
      setDocuments(prev => [newDoc, ...prev]);

      toast({ title: 'Success', description: 'File uploaded securely.' });
    } catch (error: any) {
      clearInterval(progressInterval);
      setUploadStatus('error');
      setUploadErrorMsg(error.message);
      toast({
        title: 'Upload Failed',
        description: error.message,
        variant: 'destructive',
      });
    } finally {
      setUploading(false);
      event.target.value = '';
    }
  };

  const handleDeleteDocument = async (doc: PersonalDocument) => {
    try {
      await deletePersonalDocument(doc.id);
      setDocuments(prev => prev.filter(d => d.id !== doc.id));
      toast({ title: 'Deleted', description: 'File removed successfully.' });
    } catch (error: any) {
      toast({
        title: 'Error',
        description: 'Failed to delete file.',
        variant: 'destructive',
      });
    }
  };

  const getFileIcon = (type: string) => {
    if (type.startsWith('image/')) return <ImageIcon className="h-8 w-8 text-indigo-500" />;
    if (type.includes('pdf')) return <FileText className="h-8 w-8 text-rose-500" />;
    return <FileIcon className="h-8 w-8 text-amber-500" />;
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const getPriorityBadgeColor = (priority: 'High' | 'Medium' | 'Low') => {
    switch (priority) {
      case 'High': return 'bg-red-500 hover:bg-red-600 text-white';
      case 'Medium': return 'bg-amber-500 hover:bg-amber-600 text-white';
      case 'Low': return 'bg-slate-500 hover:bg-slate-600 text-white';
    }
  };

  // Maps coordinates check
  const isCoordinates = (str: string) => {
    const regex = /^[-+]?([1-8]?\d(\.\d+)?|90(\.0+)?),\s*[-+]?(180(\.0+)?|((1[0-7]\d)|([1-9]?\d))(\.\d+)?)$/;
    return regex.test(str.trim());
  };

  const handlePreview = (doc: PersonalDocument) => {
    setPreviewDoc(doc);
    setIsPreviewOpen(true);
    setZoomLevel(1);
    setPanPosition({ x: 0, y: 0 });
    setNumPages(null);
  };

  // Zoom and Pan for Previews
  const handleWheel = (e: React.WheelEvent) => {
    if (previewDoc?.type.startsWith('image/') || previewDoc?.type === 'application/pdf') {
      e.preventDefault();
      const zoomChange = e.deltaY * -0.001;
      setZoomLevel(prev => Math.min(Math.max(0.5, prev + zoomChange), 4));
    }
  };

  const handleMouseDown = (e: React.MouseEvent) => {
    if (zoomLevel > 1) {
      setIsDragging(true);
      setDragStart({ x: e.clientX - panPosition.x, y: e.clientY - panPosition.y });
    }
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (isDragging && zoomLevel > 1) {
      setPanPosition({
        x: e.clientX - dragStart.x,
        y: e.clientY - dragStart.y
      });
    }
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  const renderPreviewContent = () => {
    if (!previewDoc) return null;
    const proxyUrl = `/api/personal-documents/${previewDoc.id}/content`;

    if (previewDoc.type.startsWith('image/')) {
      return (
        <div
          className="flex justify-center items-center w-full h-full bg-black/95 backdrop-blur-md overflow-hidden relative"
          onWheel={handleWheel}
          onMouseDown={handleMouseDown}
          onMouseMove={handleMouseMove}
          onMouseUp={handleMouseUp}
          onMouseLeave={handleMouseUp}
        >
          <div
            className="absolute inset-0 bg-cover bg-center opacity-20 blur-3xl scale-150 pointer-events-none"
            style={{ backgroundImage: `url(${proxyUrl})` }}
          />
          <img
            src={proxyUrl}
            alt={previewDoc.name}
            className="relative z-10 max-w-full max-h-full object-contain transition-transform duration-100 ease-out cursor-grab active:cursor-grabbing"
            style={{
              transform: `scale(${zoomLevel}) translate(${panPosition.x / zoomLevel}px, ${panPosition.y / zoomLevel}px)`,
              touchAction: 'none'
            }}
            draggable={false}
          />
        </div>
      );
    }

    if (previewDoc.type === 'application/pdf') {
      return (
        <div
          className="flex justify-center items-center w-full h-full bg-zinc-900 overflow-hidden relative"
          onWheel={handleWheel}
          onMouseDown={handleMouseDown}
          onMouseMove={handleMouseMove}
          onMouseUp={handleMouseUp}
          onMouseLeave={handleMouseUp}
        >
          <div
            className="relative z-10 transition-transform duration-100 ease-out cursor-grab active:cursor-grabbing"
            style={{
              transform: `scale(${zoomLevel}) translate(${panPosition.x / zoomLevel}px, ${panPosition.y / zoomLevel}px)`,
              touchAction: 'none'
            }}
          >
            <PdfDocument
              file={proxyUrl}
              onLoadSuccess={onDocumentLoadSuccess}
              loading={<div className="text-white">Loading PDF...</div>}
              error={<div className="text-red-500">Failed to load PDF</div>}
            >
              <PdfPage
                pageNumber={1}
                renderTextLayer={false}
                renderAnnotationLayer={false}
                className="shadow-2xl"
                width={pdfWidth}
              />
            </PdfDocument>
          </div>
        </div>
      );
    }

    return (
      <div className="flex flex-col items-center justify-center h-[40vh] text-center space-y-4">
        <FileIcon className="h-16 w-16 text-muted-foreground" />
        <div>
          <p className="text-lg font-medium">Preview not available</p>
          <p className="text-sm text-muted-foreground">This file type cannot be previewed directly.</p>
        </div>
        <Button asChild>
          <a href={proxyUrl} target="_blank" rel="noopener noreferrer">
            <ExternalLink className="h-4 w-4 mr-2" />
            Open File
          </a>
        </Button>
      </div>
    );
  };

  return (
    <div className="flex flex-col h-full bg-transparent p-4 md:p-6 space-y-6">
      <UploadProgressPopup
        progress={uploadProgress}
        fileName={uploadFileName}
        isVisible={showProgressPopup}
        status={uploadStatus}
        errorMessage={uploadErrorMsg}
        onClose={() => setShowProgressPopup(false)}
      />

      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight font-headline flex items-center gap-2">
            <Wallet className="h-6 w-6 text-primary" />
            Personal Pouch
          </h1>
          <p className="text-muted-foreground">A secure, private repository for your personal items.</p>
        </div>
      </div>

      <Tabs defaultValue="notes" className="w-full">
        <TabsList className="grid w-[400px] grid-cols-2 mb-6">
          <TabsTrigger value="notes">Notes & Reminders</TabsTrigger>
          <TabsTrigger value="documents">Secure Documents</TabsTrigger>
        </TabsList>

        {/* Notes Content */}
        <TabsContent value="notes" className="space-y-4">
          <div className="flex justify-between items-center">
            <h2 className="text-lg font-semibold font-headline">Private Reminders</h2>
            <Button onClick={() => handleOpenNoteDialog()}>
              <Plus className="h-4 w-4 mr-2" />
              Add Reminder
            </Button>
          </div>

          {loadingNotes ? (
            <div className="flex justify-center py-12">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          ) : notes.length === 0 ? (
            <Card className="glass-card flex flex-col items-center justify-center p-12 text-center">
              <Calendar className="h-12 w-12 text-muted-foreground mb-4" />
              <CardTitle>No Reminders Found</CardTitle>
              <CardDescription className="mt-2">
                Keep track of your personal notes, locations, and tasks.
              </CardDescription>
            </Card>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {notes.map(note => (
                <Card
                  key={note.id}
                  className={`glass-card group relative overflow-hidden border-l-4 ${
                    note.is_completed ? 'border-l-muted opacity-70' : 
                    note.priority === 'High' ? 'border-l-red-500/80' :
                    note.priority === 'Medium' ? 'border-l-amber-500/80' : 'border-l-blue-500/80'
                  }`}
                >
                  <CardHeader className="pb-2">
                    <div className="flex justify-between items-start">
                      <div className="flex items-center gap-3">
                        <button
                          onClick={() => handleToggleComplete(note)}
                          className="text-muted-foreground hover:text-primary transition-colors shrink-0"
                        >
                          {note.is_completed ? (
                            <CheckCircle2 className="h-5 w-5 text-green-500 fill-green-100" />
                          ) : (
                            <Circle className="h-5 w-5" />
                          )}
                        </button>
                        <CardTitle className={`text-base font-semibold truncate max-w-[180px] ${note.is_completed ? 'line-through text-muted-foreground' : ''}`}>
                          {note.title}
                        </CardTitle>
                      </div>
                      <Badge className={getPriorityBadgeColor(note.priority)} variant="secondary">
                        {note.priority}
                      </Badge>
                    </div>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <p className={`text-sm text-muted-foreground min-h-[40px] line-clamp-2 ${note.is_completed ? 'line-through' : ''}`}>
                      {note.description || 'No description provided.'}
                    </p>

                    <div className="flex flex-wrap gap-x-4 gap-y-2 pt-2 text-xs border-t text-muted-foreground">
                      {note.location && (
                        <div className="flex items-center gap-1.5">
                          <MapPin className="h-3.5 w-3.5 text-primary shrink-0" />
                          <a
                            href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(note.location)}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="hover:underline hover:text-primary flex items-center gap-1 truncate max-w-[120px]"
                            onClick={e => e.stopPropagation()}
                          >
                            {note.location}
                            <ExternalLink className="h-2.5 w-2.5 inline" />
                          </a>
                        </div>
                      )}

                      {note.due_date && (
                        <div className="flex items-center gap-1.5 ml-auto">
                          <Calendar className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                          <span>{format(new Date(note.due_date), 'yyyy-MM-dd')}</span>
                        </div>
                      )}
                    </div>

                    <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity flex gap-1">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 hover:bg-muted"
                        onClick={() => handleOpenNoteDialog(note)}
                      >
                        <Pencil className="h-4 w-4 text-muted-foreground hover:text-foreground" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 hover:bg-destructive/10"
                        onClick={() => handleDeleteNote(note.id)}
                      >
                        <Trash2 className="h-4 w-4 text-destructive" />
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </TabsContent>

        {/* Documents Content */}
        <TabsContent value="documents" className="space-y-4">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div className="flex items-center gap-2">
              <h2 className="text-lg font-semibold font-headline">Private Documents</h2>
              <Select value={selectedCategory} onValueChange={setSelectedCategory}>
                <SelectTrigger className="w-[150px] bg-background">
                  <SelectValue placeholder="Category" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="All">All Categories</SelectItem>
                  <SelectItem value="General">General</SelectItem>
                  <SelectItem value="Contracts">Contracts</SelectItem>
                  <SelectItem value="Tax Forms">Tax Forms</SelectItem>
                  <SelectItem value="IDs">IDs</SelectItem>
                  <SelectItem value="Invoices">Invoices</SelectItem>
                  <SelectItem value="Certificates">Certificates</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="flex items-center gap-2 w-full sm:w-auto">
              <Select value={newDocCategory} onValueChange={setNewDocCategory}>
                <SelectTrigger className="w-[150px] bg-background">
                  <SelectValue placeholder="Upload to Category" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="General">General</SelectItem>
                  <SelectItem value="Contracts">Contracts</SelectItem>
                  <SelectItem value="Tax Forms">Tax Forms</SelectItem>
                  <SelectItem value="IDs">IDs</SelectItem>
                  <SelectItem value="Invoices">Invoices</SelectItem>
                  <SelectItem value="Certificates">Certificates</SelectItem>
                </SelectContent>
              </Select>

              <div className="relative">
                <input
                  type="file"
                  id="personal-doc-upload"
                  className="hidden"
                  onChange={handleFileUpload}
                  disabled={uploading}
                />
                <label htmlFor="personal-doc-upload">
                  <Button variant="default" className="cursor-pointer" asChild disabled={uploading}>
                    <span>
                      {uploading ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : <Upload className="h-4 w-4 mr-2" />}
                      Upload File
                    </span>
                  </Button>
                </label>
              </div>
            </div>
          </div>

          {loadingDocs ? (
            <div className="flex justify-center py-12">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          ) : documents.length === 0 ? (
            <Card className="glass-card flex flex-col items-center justify-center p-12 text-center">
              <FileText className="h-12 w-12 text-muted-foreground mb-4" />
              <CardTitle>No Documents Found</CardTitle>
              <CardDescription className="mt-2">
                Securely store personal files, tax records, and certificates.
              </CardDescription>
            </Card>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {documents
                .filter(doc => selectedCategory === 'All' || doc.category === selectedCategory)
                .map(doc => (
                  <Card
                    key={doc.id}
                    className="glass-card group relative overflow-hidden cursor-pointer"
                    onClick={() => handlePreview(doc)}
                  >
                    <CardHeader className="pb-2">
                      <div className="flex items-center gap-3">
                        {getFileIcon(doc.type)}
                        <div className="space-y-1 overflow-hidden">
                          <CardTitle className="text-base font-medium truncate max-w-[150px]" title={doc.name}>
                            {doc.name}
                          </CardTitle>
                          <CardDescription className="text-xs flex items-center gap-2">
                            <Badge variant="outline" className="text-[10px] px-1 py-0 h-5">{doc.category || 'General'}</Badge>
                            <span>{formatFileSize(doc.size)}</span>
                          </CardDescription>
                        </div>
                      </div>
                    </CardHeader>
                    <CardContent>
                      <div className="flex items-center justify-between mt-4">
                        <div className="text-xs text-muted-foreground">
                          {format(new Date(doc.created_at), 'yyyy-MM-dd')}
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
                            <a href={`/api/personal-documents/${doc.id}/content`} target="_blank" rel="noopener noreferrer" download={doc.name}>
                              <Download className="h-4 w-4" />
                            </a>
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 text-destructive hover:text-destructive/90"
                            onClick={(e) => { e.stopPropagation(); handleDeleteDocument(doc); }}
                            title="Delete"
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                ))}
            </div>
          )}
        </TabsContent>
      </Tabs>

      {/* Reminder Add/Edit Dialog */}
      <Dialog open={isNoteDialogOpen} onOpenChange={setIsNoteDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>{editingNote ? 'Edit Reminder' : 'Add New Reminder'}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-1">
              <Label htmlFor="title">Title</Label>
              <Input
                id="title"
                value={noteTitle}
                onChange={e => setNoteTitle(e.target.value)}
                placeholder="e.g. Call cement vendor"
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                value={noteDescription}
                onChange={e => setNoteDescription(e.target.value)}
                placeholder="Details about this task..."
                rows={3}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1">
                <Label htmlFor="priority">Priority</Label>
                <Select value={notePriority} onValueChange={(val: any) => setNotePriority(val)}>
                  <SelectTrigger id="priority">
                    <SelectValue placeholder="Priority" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="High">High</SelectItem>
                    <SelectItem value="Medium">Medium</SelectItem>
                    <SelectItem value="Low">Low</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1">
                <Label htmlFor="due_date">Due Date</Label>
                <Input
                  id="due_date"
                  type="date"
                  value={noteDueDate}
                  onChange={e => setNoteDueDate(e.target.value)}
                />
              </div>
            </div>
            <div className="space-y-1">
              <Label htmlFor="location">Location</Label>
              <Input
                id="location"
                value={noteLocation}
                onChange={e => setNoteLocation(e.target.value)}
                placeholder="e.g. Home, Grocery Store or 13.0827,80.2707"
              />
              <span className="text-[10px] text-muted-foreground block">
                Supports location names or lat/long pairs for navigation map links.
              </span>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsNoteDialogOpen(false)}>Cancel</Button>
            <Button onClick={handleSaveNote} disabled={isPending}>
              {isPending && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
              {editingNote ? 'Save Changes' : 'Create Reminder'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Fullscreen Preview Dialog */}
      <Dialog open={isPreviewOpen} onOpenChange={setIsPreviewOpen}>
        <DialogContent className="max-w-none w-screen h-screen m-0 p-0 rounded-none bg-background/95 backdrop-blur-xl border-none flex flex-col">
          <div className="absolute top-4 right-4 z-50 flex gap-2">
            {previewDoc && (
              <Button variant="secondary" size="icon" className="rounded-full shadow-lg" asChild>
                <a href={`/api/personal-documents/${previewDoc.id}/content`} target="_blank" rel="noopener noreferrer">
                  <ExternalLink className="h-4 w-4" />
                </a>
              </Button>
            )}
            <Button
              variant="destructive"
              size="icon"
              className="rounded-full shadow-lg"
              onClick={() => setIsPreviewOpen(false)}
            >
              <X className="h-4 w-4" />
            </Button>
          </div>

          <div className="flex-1 w-full h-full overflow-hidden flex items-center justify-center relative">
            {renderPreviewContent()}
          </div>

          <div className="absolute bottom-0 left-0 right-0 p-4 bg-gradient-to-t from-black/80 to-transparent text-white pointer-events-none">
            <div className="container mx-auto max-w-4xl">
              <h2 className="text-xl font-bold truncate">{previewDoc?.name}</h2>
              <div className="flex items-center gap-4 text-sm opacity-80">
                <Badge variant="secondary" className="bg-white/20 hover:bg-white/30 text-white border-none">{previewDoc?.category}</Badge>
                <span>{previewDoc && formatFileSize(previewDoc.size)}</span>
                <span>•</span>
                <span>Uploaded on {previewDoc && format(new Date(previewDoc.created_at), 'yyyy-MM-dd')}</span>
              </div>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
