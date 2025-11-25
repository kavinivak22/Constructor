'use client';

import { useState, useEffect } from 'react';
import { useSupabase } from '@/supabase/provider';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { useToast } from '@/hooks/use-toast';
import { Loader2, Upload, FileText, Trash2, Download, File as FileIcon, Image as ImageIcon } from 'lucide-react';
import { format } from 'date-fns';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Document, Project } from '@/lib/data';
import { UploadProgressPopup } from '@/components/upload-progress-popup';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogClose } from '@/components/ui/dialog';
import { Eye, ExternalLink, X, Pencil, MoreVertical, Check } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Label } from '@/components/ui/label';
import { Document as PdfDocument, Page as PdfPage, pdfjs } from 'react-pdf';
// Configure PDF worker
pdfjs.GlobalWorkerOptions.workerSrc = `//unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;

export default function ProjectPouchPage() {
  const { supabase, user } = useSupabase();
  const { toast } = useToast();
  const [documents, setDocuments] = useState<Document[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProject, setSelectedProject] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);

  // Upload Progress State
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadStatus, setUploadStatus] = useState<'uploading' | 'completed' | 'error'>('uploading');
  const [uploadFileName, setUploadFileName] = useState('');
  const [showProgressPopup, setShowProgressPopup] = useState(false);
  const [uploadErrorMsg, setUploadErrorMsg] = useState<string | undefined>(undefined);
  const [selectedCategory, setSelectedCategory] = useState<string>('All');

  // Edit State
  const [editingDoc, setEditingDoc] = useState<Document | null>(null);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [editName, setEditName] = useState('');
  const [editCategory, setEditCategory] = useState('');

  // Preview State
  const [previewDoc, setPreviewDoc] = useState<Document | null>(null);
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

    // Initial set
    updateWidth();

    window.addEventListener('resize', updateWidth);
    return () => window.removeEventListener('resize', updateWidth);
  }, []);

  useEffect(() => {
    if (user) {
      fetchProjects();
    }
  }, [user]);

  useEffect(() => {
    if (selectedProject) {
      fetchDocuments(selectedProject);
    } else {
      setDocuments([]);
    }
  }, [selectedProject]);

  const fetchProjects = async () => {
    try {
      // Fetch projects where the user is a member (using projectIds array in users table)
      const { data: userData, error: userError } = await supabase
        .from('users')
        .select('projectIds')
        .eq('id', user?.id)
        .single();

      if (userError) throw userError;

      if (userData?.projectIds && userData.projectIds.length > 0) {
        const { data: projectsData, error: projectsError } = await supabase
          .from('projects')
          .select('*')
          .in('id', userData.projectIds);

        if (projectsError) throw projectsError;
        setProjects(projectsData || []);
        if (projectsData && projectsData.length > 0) {
          setSelectedProject(projectsData[0].id);
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
        .select('*, uploader:users(displayName, photoURL)')
        .eq('project_id', projectId)
        .order('created_at', { ascending: false });

      if (error) throw error;

      // Fetch read status for current user
      const { data: readData, error: readError } = await supabase
        .from('document_reads')
        .select('document_id')
        .eq('user_id', user?.id);

      const readDocIds = new Set((readData || []).map((r: any) => r.document_id));

      // Map database columns to Document type
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
          photoURL: doc.uploader?.photoURL
        }
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

    setUploading(true);

    // Reset Progress State
    setUploadProgress(0);
    setUploadStatus('uploading');
    setUploadFileName(file.name);
    setUploadErrorMsg(undefined);
    setShowProgressPopup(true);

    // Simulate progress
    const progressInterval = setInterval(() => {
      setUploadProgress((prev) => {
        if (prev >= 90) return prev;
        return prev + 10;
      });
    }, 200);

    try {
      const fileExt = file.name.split('.').pop();
      const fileName = `${selectedProject}/${Math.random()}.${fileExt}`;

      // Upload to Storage
      const { error: uploadError } = await supabase.storage
        .from('project-documents')
        .upload(fileName, file);

      if (uploadError) throw uploadError;

      const { data: { publicUrl } } = supabase.storage
        .from('project-documents')
        .getPublicUrl(fileName);

      // Save Metadata to Database
      const { error: dbError } = await supabase
        .from('documents')
        .insert([
          {
            project_id: selectedProject,
            uploader_id: user?.id,
            name: file.name,
            url: publicUrl,
            size: file.size,
            type: file.type,
            category: selectedCategory === 'All' ? 'General' : selectedCategory,
          }
        ]);

      if (dbError) throw dbError;

      // Complete progress
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
        description: error.message,
        variant: 'destructive',
      });
    } finally {
      setUploading(false);
      // Reset input
      event.target.value = '';
    }
  };

  const deleteDocument = async (doc: Document) => {
    try {
      // Extract path from URL or store path in DB. 
      // For now assuming we can't easily delete from storage without path, 
      // but we can delete from DB to hide it. 
      // Ideally we store the storage path in the DB too.

      const { error } = await supabase
        .from('documents')
        .delete()
        .eq('id', doc.id);

      if (error) throw error;

      setDocuments(documents.filter(d => d.id !== doc.id));
      toast({
        title: 'Deleted',
        description: 'Document removed successfully.',
      });
    } catch (error: any) {
      toast({
        title: 'Error',
        description: 'Failed to delete document.',
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

      setDocuments(documents.map(d =>
        d.id === editingDoc.id ? { ...d, name: editName, category: editCategory } : d
      ));

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

  const markAsRead = async (docId: string) => {
    try {
      const { error } = await supabase
        .from('document_reads')
        .insert([{ document_id: docId, user_id: user?.id }])
        .select(); // Use select to ignore duplicate error if needed or handle gracefully

      // Optimistically update UI
      setDocuments(documents.map(d => d.id === docId ? { ...d, isRead: true } : d));
    } catch (error) {
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
    setZoomLevel(1);
    setPanPosition({ x: 0, y: 0 });
    setNumPages(null);
    if (!doc.isRead) {
      markAsRead(doc.id);
    }
  };

  // Zoom and Pan Handlers
  const handleWheel = (e: React.WheelEvent) => {
    // Enable zoom for both images and PDFs
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

  // Touch handlers for mobile pinch zoom and pan would go here
  // For simplicity in this implementation, we'll rely on native browser behavior for some parts 
  // or simple double tap to zoom if we were using a library. 
  // Since we are custom, let's add basic touch support.

  const [lastTouchDistance, setLastTouchDistance] = useState<number | null>(null);

  const handleTouchStart = (e: React.TouchEvent) => {
    if (e.touches.length === 2) {
      const dist = Math.hypot(
        e.touches[0].clientX - e.touches[1].clientX,
        e.touches[0].clientY - e.touches[1].clientY
      );
      setLastTouchDistance(dist);
    } else if (e.touches.length === 1 && zoomLevel > 1) {
      setIsDragging(true);
      setDragStart({ x: e.touches[0].clientX - panPosition.x, y: e.touches[0].clientY - panPosition.y });
    }
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (e.touches.length === 2 && lastTouchDistance) {
      const dist = Math.hypot(
        e.touches[0].clientX - e.touches[1].clientX,
        e.touches[0].clientY - e.touches[1].clientY
      );
      const delta = dist - lastTouchDistance;
      setZoomLevel(prev => Math.min(Math.max(0.5, prev + delta * 0.01), 4));
      setLastTouchDistance(dist);
    } else if (e.touches.length === 1 && isDragging && zoomLevel > 1) {
      setPanPosition({
        x: e.touches[0].clientX - dragStart.x,
        y: e.touches[0].clientY - dragStart.y
      });
    }
  };

  const handleTouchEnd = () => {
    setLastTouchDistance(null);
    setIsDragging(false);
  };

  const renderPreviewContent = () => {
    if (!previewDoc) return null;

    const proxyUrl = `/api/documents/${previewDoc.id}/content`;

    if (previewDoc.type.startsWith('image/')) {
      return (
        <div
          className="flex justify-center items-center w-full h-full bg-black/95 backdrop-blur-md overflow-hidden relative group"
          onWheel={handleWheel}
          onMouseDown={handleMouseDown}
          onMouseMove={handleMouseMove}
          onMouseUp={handleMouseUp}
          onMouseLeave={handleMouseUp}
          onTouchStart={handleTouchStart}
          onTouchMove={handleTouchMove}
          onTouchEnd={handleTouchEnd}
        >
          {/* Blurred Background */}
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
          onTouchStart={handleTouchStart}
          onTouchMove={handleTouchMove}
          onTouchEnd={handleTouchEnd}
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
    <div className="flex flex-col h-full bg-secondary/10 p-4 md:p-6 space-y-6">
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
          <h1 className="text-2xl font-bold tracking-tight font-headline">Project Pouch</h1>
          <p className="text-muted-foreground">Manage and share project documents.</p>
        </div>

        <div className="flex items-center gap-2 w-full md:w-auto">
          <Select value={selectedProject} onValueChange={setSelectedProject}>
            <SelectTrigger className="w-[200px] bg-background">
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

          <Select value={selectedCategory} onValueChange={setSelectedCategory}>
            <SelectTrigger className="w-[150px] bg-background">
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

          <div className="relative">
            <input
              type="file"
              id="doc-upload"
              className="hidden"
              onChange={handleFileUpload}
              disabled={uploading || !selectedProject}
            />
            <label htmlFor="doc-upload">
              <Button
                variant="default"
                className="cursor-pointer"
                asChild
                disabled={uploading || !selectedProject}
              >
                <span>
                  {uploading ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : <Upload className="h-4 w-4 mr-2" />}
                  Upload
                </span>
              </Button>
            </label>
          </div>
        </div>
      </div >

      {!selectedProject ? (
        <Card className="flex flex-col items-center justify-center h-[400px] text-center p-6">
          <div className="bg-primary/10 p-4 rounded-full mb-4">
            <FileText className="h-12 w-12 text-primary" />
          </div>
          <h3 className="text-lg font-semibold">No Project Selected</h3>
          <p className="text-muted-foreground max-w-sm mt-2">
            Please select a project from the dropdown above to view and manage its documents.
          </p>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {loading ? (
            <div className="col-span-full flex justify-center py-12">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          ) : documents.length === 0 ? (
            <div className="col-span-full flex flex-col items-center justify-center py-12 text-center">
              <p className="text-muted-foreground">No documents found for this project.</p>
              <p className="text-sm text-muted-foreground">Upload a file to get started.</p>
            </div>
          ) : (
            documents
              .filter(doc => selectedCategory === 'All' || doc.category === selectedCategory)
              .map((doc) => (
                <Card
                  key={doc.id}
                  className="hover:shadow-lg transition-all duration-300 group relative overflow-hidden cursor-pointer border-transparent hover:border-primary/20"
                  onClick={() => handlePreview(doc)}
                >
                  {!doc.isRead && (
                    <div className="absolute top-2 right-2 z-10">
                      <Badge className="bg-blue-500 hover:bg-blue-600 text-[10px] px-2 py-0.5">NEW</Badge>
                    </div>
                  )}
                  <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-2">
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
                      <div className="flex items-center gap-2 text-xs text-muted-foreground">
                        <span>{format(new Date(typeof doc.createdAt === 'string' ? doc.createdAt : (doc.createdAt as any).seconds * 1000), 'MMM d')}</span>
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
                          <a href={`/api/documents/${doc.id}/content`} target="_blank" rel="noopener noreferrer" download={doc.name}>
                            <Download className="h-4 w-4" />
                          </a>
                        </Button>

                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 hover:text-primary"
                          onClick={(e) => { e.stopPropagation(); handleEdit(doc); }}
                          title="Edit"
                        >
                          <Pencil className="h-4 w-4" />
                        </Button>

                        {(user?.id === doc.uploaderId || user?.role === 'admin') && (
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 text-destructive hover:text-destructive/90"
                            onClick={(e) => { e.stopPropagation(); deleteDocument(doc); }}
                            title="Delete"
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        )}
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))
          )}
        </div>
      )}

      {/* Fullscreen Preview Dialog */}
      <Dialog open={isPreviewOpen} onOpenChange={setIsPreviewOpen}>
        <DialogContent className="max-w-none w-screen h-screen m-0 p-0 rounded-none bg-background/95 backdrop-blur-xl border-none flex flex-col">
          <div className="absolute top-4 right-4 z-50 flex gap-2">
            {previewDoc && (
              <Button variant="secondary" size="icon" className="rounded-full shadow-lg" asChild>
                <a href={`/api/documents/${previewDoc.id}/content`} target="_blank" rel="noopener noreferrer">
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
                <span>Uploaded by {previewDoc?.uploader?.name}</span>
              </div>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Edit Document Dialog */}
      <Dialog open={isEditOpen} onOpenChange={setIsEditOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Edit Document</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="name">Document Name</Label>
              <Input id="name" value={editName} onChange={(e) => setEditName(e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="category">Category</Label>
              <Select value={editCategory} onValueChange={setEditCategory}>
                <SelectTrigger>
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
            <Button variant="outline" onClick={() => setIsEditOpen(false)}>Cancel</Button>
            <Button onClick={saveEdit}>Save Changes</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
