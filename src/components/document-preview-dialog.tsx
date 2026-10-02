'use client';

import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  X,
  Download,
  ExternalLink,
  ZoomIn,
  ZoomOut,
  RotateCw,
  Maximize2,
  ChevronLeft,
  ChevronRight,
  FileText,
  Image as ImageIcon,
  File as FileIcon,
  Loader2,
} from 'lucide-react';
import { Document as PdfDocument, Page as PdfPage, pdfjs } from 'react-pdf';

// Ensure PDF worker is configured
pdfjs.GlobalWorkerOptions.workerSrc = `//unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;

export interface DocumentPreviewItem {
  id: string;
  name: string;
  url: string;
  size: number;
  type: string;
  category?: string;
  createdAt: string;
  uploader?: {
    name: string;
    photoURL?: string | null;
  };
}

interface DocumentPreviewDialogProps {
  isOpen: boolean;
  onClose: () => void;
  document: DocumentPreviewItem | null;
  documents?: DocumentPreviewItem[];
  onSelectDocument?: (doc: DocumentPreviewItem) => void;
  apiProxyPath?: (docId: string) => string;
}

export function DocumentPreviewDialog({
  isOpen,
  onClose,
  document,
  documents = [],
  onSelectDocument,
  apiProxyPath = (id) => `/api/documents/${id}/content`,
}: DocumentPreviewDialogProps) {
  // Zoom, pan, rotation states
  const [zoomLevel, setZoomLevel] = useState<number>(1);
  const [panPosition, setPanPosition] = useState<{ x: 0; y: 0 }>({ x: 0, y: 0 });
  const [rotation, setRotation] = useState<number>(0);
  const [isDragging, setIsDragging] = useState<boolean>(false);
  const [dragStart, setDragStart] = useState<{ x: 0; y: 0 }>({ x: 0, y: 0 });

  // Multi-page PDF state
  const [numPages, setNumPages] = useState<number | null>(null);
  const [pageNumber, setPageNumber] = useState<number>(1);
  const [pdfWidth, setPdfWidth] = useState<number>(600);

  // Touch tracking for pinch-zoom and double-tap
  const [lastTouchDistance, setLastTouchDistance] = useState<number | null>(null);
  const lastTapRef = useRef<number>(0);
  const touchStartPosRef = useRef<{ x: number; y: number }>({ x: 0, y: 0 });

  // Filter out notes so only previewable files are flipped through
  const fileDocuments = documents.filter((d) => d.type !== 'note');
  const currentIndex = document ? fileDocuments.findIndex((d) => d.id === document.id) : -1;
  const hasPrevDoc = currentIndex > 0;
  const hasNextDoc = currentIndex !== -1 && currentIndex < fileDocuments.length - 1;

  // Reset viewport whenever current document changes
  const resetView = useCallback(() => {
    setZoomLevel(1);
    setPanPosition({ x: 0, y: 0 });
    setRotation(0);
    setPageNumber(1);
    setNumPages(null);
  }, []);

  useEffect(() => {
    if (document) {
      resetView();
    }
  }, [document?.id, resetView]);

  // Handle responsive PDF width
  useEffect(() => {
    const handleResize = () => {
      if (typeof window !== 'undefined') {
        const w = window.innerWidth;
        if (w < 640) {
          // Mobile edge-to-edge
          setPdfWidth(Math.min(w * 0.95, 420));
        } else if (w < 1024) {
          setPdfWidth(Math.min(w * 0.8, 650));
        } else {
          setPdfWidth(Math.min(w * 0.65, 820));
        }
      }
    };

    handleResize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Keyboard navigation
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      // Don't intercept if user is inside an input
      if (['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement)?.tagName)) return;

      if (e.key === 'Escape') {
        onClose();
      } else if (e.key === 'ArrowLeft' && hasPrevDoc && onSelectDocument) {
        onSelectDocument(fileDocuments[currentIndex - 1]);
      } else if (e.key === 'ArrowRight' && hasNextDoc && onSelectDocument) {
        onSelectDocument(fileDocuments[currentIndex + 1]);
      } else if (e.key === '+' || e.key === '=') {
        setZoomLevel((prev) => Math.min(prev + 0.25, 4));
      } else if (e.key === '-') {
        setZoomLevel((prev) => Math.max(prev - 0.25, 0.5));
      } else if (e.key.toLowerCase() === 'r') {
        setRotation((r) => (r + 90) % 360);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, hasPrevDoc, hasNextDoc, currentIndex, fileDocuments, onSelectDocument, onClose]);

  const handlePrevDoc = (e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    if (hasPrevDoc && onSelectDocument) {
      onSelectDocument(fileDocuments[currentIndex - 1]);
    }
  };

  const handleNextDoc = (e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    if (hasNextDoc && onSelectDocument) {
      onSelectDocument(fileDocuments[currentIndex + 1]);
    }
  };

  const handleRotate = (e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    setRotation((prev) => (prev + 90) % 360);
  };

  const handleZoomIn = (e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    setZoomLevel((prev) => Math.min(prev + 0.25, 4));
  };

  const handleZoomOut = (e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    setZoomLevel((prev) => Math.max(prev - 0.25, 0.5));
  };

  const handleResetZoom = (e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    setZoomLevel(1);
    setPanPosition({ x: 0, y: 0 });
  };

  // Mouse wheel zoom
  const handleWheel = (e: React.WheelEvent) => {
    e.preventDefault();
    const zoomChange = e.deltaY * -0.0015;
    setZoomLevel((prev) => Math.min(Math.max(0.5, prev + zoomChange), 4));
  };

  // Mouse pan
  const handleMouseDown = (e: React.MouseEvent) => {
    if (zoomLevel > 1) {
      setIsDragging(true);
      setDragStart({ x: e.clientX - panPosition.x, y: e.clientY - panPosition.y });
    }
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (isDragging && zoomLevel > 1) {
      setPanPosition({
        x: (e.clientX - dragStart.x) as any,
        y: (e.clientY - dragStart.y) as any,
      });
    }
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  // Touch handlers (Mobile pinch-to-zoom, pan, double-tap, and swipe document)
  const handleTouchStart = (e: React.TouchEvent) => {
    if (e.touches.length === 2) {
      const dist = Math.hypot(
        e.touches[0].clientX - e.touches[1].clientX,
        e.touches[0].clientY - e.touches[1].clientY
      );
      setLastTouchDistance(dist);
    } else if (e.touches.length === 1) {
      const touch = e.touches[0];
      touchStartPosRef.current = { x: touch.clientX, y: touch.clientY };

      // Double-tap recognition
      const now = Date.now();
      if (now - lastTapRef.current < 300) {
        // Toggle zoom between 1x and 2x
        setZoomLevel((z) => (z > 1.2 ? 1 : 2));
        setPanPosition({ x: 0, y: 0 });
        lastTapRef.current = 0;
        return;
      }
      lastTapRef.current = now;

      if (zoomLevel > 1) {
        setIsDragging(true);
        setDragStart({
          x: (touch.clientX - panPosition.x) as any,
          y: (touch.clientY - panPosition.y) as any,
        });
      }
    }
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (e.touches.length === 2 && lastTouchDistance) {
      const dist = Math.hypot(
        e.touches[0].clientX - e.touches[1].clientX,
        e.touches[0].clientY - e.touches[1].clientY
      );
      const delta = dist - lastTouchDistance;
      setZoomLevel((prev) => Math.min(Math.max(0.5, prev + delta * 0.01), 4));
      setLastTouchDistance(dist);
    } else if (e.touches.length === 1 && isDragging && zoomLevel > 1) {
      const touch = e.touches[0];
      setPanPosition({
        x: (touch.clientX - dragStart.x) as any,
        y: (touch.clientY - dragStart.y) as any,
      });
    }
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    // If not zoomed in, detect swipe to navigate documents
    if (zoomLevel <= 1.05 && e.changedTouches.length === 1) {
      const touch = e.changedTouches[0];
      const deltaX = touch.clientX - touchStartPosRef.current.x;
      const deltaY = touch.clientY - touchStartPosRef.current.y;

      if (Math.abs(deltaX) > 65 && Math.abs(deltaY) < 60) {
        if (deltaX < 0 && hasNextDoc && onSelectDocument) {
          onSelectDocument(fileDocuments[currentIndex + 1]);
        } else if (deltaX > 0 && hasPrevDoc && onSelectDocument) {
          onSelectDocument(fileDocuments[currentIndex - 1]);
        }
      }
    }

    setLastTouchDistance(null);
    setIsDragging(false);
  };

  if (!document) return null;

  const proxyUrl = apiProxyPath(document.id);
  const isImage = document.type.startsWith('image/');
  const isPdf = document.type === 'application/pdf' || document.name.toLowerCase().endsWith('.pdf');

  const formatFileSize = (bytes: number) => {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  };

  const getFileBadgeIcon = () => {
    if (isImage) return <ImageIcon className="h-4 w-4 text-sky-400" />;
    if (isPdf) return <FileText className="h-4 w-4 text-rose-400" />;
    return <FileIcon className="h-4 w-4 text-zinc-400" />;
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent
        className="max-w-none w-screen h-screen m-0 p-0 rounded-none bg-zinc-950 text-zinc-100 border-none flex flex-col overflow-hidden select-none outline-none shadow-none"
      >
        {/* Screen Reader Header (Radix UI compliant) */}
        <DialogHeader className="sr-only">
          <DialogTitle>{document.name}</DialogTitle>
          <DialogDescription>
            Previewing {document.category || 'project document'} ({formatFileSize(document.size)})
          </DialogDescription>
        </DialogHeader>

        {/* 1. TOP HEADER BAR: Standard Modern Navigation Bar */}
        <div className="relative z-50 flex items-center justify-between px-3 sm:px-5 py-2.5 sm:py-3 bg-zinc-900/90 backdrop-blur-xl border-b border-white/10 shrink-0">
          {/* Left: Back / Close & Document Title */}
          <div className="flex items-center gap-2.5 sm:gap-3 min-w-0 mr-2 flex-1">
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8 sm:h-9 sm:w-9 rounded-full text-zinc-300 hover:text-white hover:bg-white/10 shrink-0"
              onClick={onClose}
              title="Close (Esc)"
            >
              <X className="h-4 w-4 sm:h-5 sm:w-5" />
            </Button>

            <div className="flex items-center gap-2 min-w-0">
              <div className="hidden xs:flex p-1.5 rounded-lg bg-white/5 border border-white/10 shrink-0">
                {getFileBadgeIcon()}
              </div>
              <div className="min-w-0 flex flex-col">
                <span
                  className="text-xs sm:text-sm font-semibold truncate text-white leading-tight max-w-[170px] sm:max-w-[320px] md:max-w-[480px]"
                  title={document.name}
                >
                  {document.name}
                </span>
                <div className="flex items-center gap-1.5 text-[10px] sm:text-xs text-zinc-400 mt-0.5 truncate">
                  <span>{formatFileSize(document.size)}</span>
                  <span>•</span>
                  <span className="truncate">{document.category || 'General'}</span>
                  {document.uploader?.name && (
                    <>
                      <span className="hidden sm:inline">•</span>
                      <span className="hidden sm:inline truncate">
                        By {document.uploader.name}
                      </span>
                    </>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Right: Quick Action Controls */}
          <div className="flex items-center gap-1 sm:gap-1.5 shrink-0">
            {/* Direct Download Button */}
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8 sm:h-9 sm:w-9 rounded-full text-zinc-300 hover:text-white hover:bg-white/10"
              asChild
              title="Download File"
            >
              <a href={proxyUrl} download={document.name} target="_blank" rel="noopener noreferrer">
                <Download className="h-4 w-4" />
              </a>
            </Button>

            {/* Open Original in New Window */}
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8 sm:h-9 sm:w-9 rounded-full text-zinc-300 hover:text-white hover:bg-white/10"
              asChild
              title="Open Original in New Tab"
            >
              <a href={proxyUrl} target="_blank" rel="noopener noreferrer">
                <ExternalLink className="h-4 w-4" />
              </a>
            </Button>
          </div>
        </div>

        {/* 2. MAIN PREVIEW CANVAS: Center Workspace with Pan & Pinch Zoom */}
        <div
          className="flex-1 w-full h-full relative overflow-hidden flex items-center justify-center bg-zinc-950 cursor-grab active:cursor-grabbing"
          onWheel={handleWheel}
          onMouseDown={handleMouseDown}
          onMouseMove={handleMouseMove}
          onMouseUp={handleMouseUp}
          onMouseLeave={handleMouseUp}
          onTouchStart={handleTouchStart}
          onTouchMove={handleTouchMove}
          onTouchEnd={handleTouchEnd}
        >
          {/* Subtle Ambient Glow for Images */}
          {isImage && (
            <div
              className="absolute inset-0 bg-cover bg-center opacity-15 blur-3xl scale-150 pointer-events-none transition-all duration-300"
              style={{ backgroundImage: `url(${proxyUrl})` }}
            />
          )}

          {/* Previous Document Edge Button (Desktop & Tablet) */}
          {hasPrevDoc && onSelectDocument && (
            <button
              onClick={handlePrevDoc}
              aria-label="Previous file"
              className="hidden sm:flex absolute left-4 z-40 h-10 w-10 rounded-full bg-zinc-900/80 hover:bg-zinc-800 text-zinc-200 hover:text-white backdrop-blur-md border border-white/10 items-center justify-center transition-all shadow-xl hover:scale-105 active:scale-95"
            >
              <ChevronLeft className="h-5 w-5" />
            </button>
          )}

          {/* Next Document Edge Button (Desktop & Tablet) */}
          {hasNextDoc && onSelectDocument && (
            <button
              onClick={handleNextDoc}
              aria-label="Next file"
              className="hidden sm:flex absolute right-4 z-40 h-10 w-10 rounded-full bg-zinc-900/80 hover:bg-zinc-800 text-zinc-200 hover:text-white backdrop-blur-md border border-white/10 items-center justify-center transition-all shadow-xl hover:scale-105 active:scale-95"
            >
              <ChevronRight className="h-5 w-5" />
            </button>
          )}

          {/* Transformed Visual Container */}
          <div
            className="relative z-10 transition-transform duration-75 ease-out flex items-center justify-center max-w-full max-h-full"
            style={{
              transform: `translate(${panPosition.x}px, ${panPosition.y}px) scale(${zoomLevel}) rotate(${rotation}deg)`,
              touchAction: 'none',
            }}
          >
            {isImage ? (
              <img
                src={proxyUrl}
                alt={document.name}
                className="max-w-[90vw] max-h-[78vh] object-contain rounded-lg shadow-2xl transition-all"
                draggable={false}
              />
            ) : isPdf ? (
              <PdfDocument
                file={proxyUrl}
                onLoadSuccess={({ numPages }) => {
                  setNumPages(numPages);
                  setPageNumber(1);
                }}
                loading={
                  <div className="flex flex-col items-center gap-2 p-8 text-zinc-400">
                    <Loader2 className="h-7 w-7 animate-spin text-primary" />
                    <span className="text-xs">Loading PDF document...</span>
                  </div>
                }
                error={
                  <div className="flex flex-col items-center justify-center p-6 bg-zinc-900/90 rounded-2xl shadow-2xl max-w-sm border border-white/10 text-center mx-4">
                    <FileText className="h-10 w-10 text-rose-500 mb-2.5" />
                    <p className="font-semibold text-white text-sm">Failed to render PDF preview</p>
                    <p className="text-xs text-zinc-400 mt-1 mb-4">
                      You can open or download the original file directly.
                    </p>
                    <div className="flex gap-2">
                      <Button size="sm" className="rounded-xl" asChild>
                        <a href={proxyUrl} target="_blank" rel="noopener noreferrer">
                          <ExternalLink className="h-3.5 w-3.5 mr-1.5" />
                          Open File
                        </a>
                      </Button>
                      {document.url && (
                        <Button
                          size="sm"
                          variant="outline"
                          className="rounded-xl border-white/20 text-zinc-200"
                          asChild
                        >
                          <a href={document.url} target="_blank" rel="noopener noreferrer">
                            Direct Link
                          </a>
                        </Button>
                      )}
                    </div>
                  </div>
                }
              >
                <PdfPage
                  pageNumber={pageNumber}
                  renderTextLayer={false}
                  renderAnnotationLayer={false}
                  className="shadow-2xl rounded-sm overflow-hidden"
                  width={pdfWidth}
                />
              </PdfDocument>
            ) : (
              <div className="flex flex-col items-center justify-center p-8 bg-zinc-900/80 rounded-2xl border border-white/10 text-center max-w-sm mx-4">
                <FileIcon className="h-12 w-12 text-zinc-400 mb-3" />
                <p className="text-base font-medium text-white">Preview not supported</p>
                <p className="text-xs text-zinc-400 mt-1 mb-4">
                  This file format cannot be viewed inline. You can download or open it directly.
                </p>
                <Button asChild size="sm" className="rounded-xl">
                  <a href={proxyUrl} target="_blank" rel="noopener noreferrer">
                    <ExternalLink className="h-3.5 w-3.5 mr-1.5" />
                    Open File
                  </a>
                </Button>
              </div>
            )}
          </div>
        </div>

        {/* 3. FLOATING GLASS BOTTOM DOCK: Modern Thumb-friendly Controls */}
        <div className="absolute bottom-4 sm:bottom-6 left-1/2 -translate-x-1/2 z-50 flex items-center gap-1 sm:gap-2 px-3 sm:px-4 py-1.5 sm:py-2 bg-zinc-900/90 hover:bg-zinc-900 text-white backdrop-blur-xl border border-white/15 rounded-full shadow-2xl transition-all">
          {/* PDF Page Navigation */}
          {isPdf && numPages && numPages > 1 && (
            <>
              <div className="flex items-center gap-1 mr-1 pr-1.5 sm:pr-2 border-r border-white/15">
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-7 w-7 rounded-full text-zinc-300 hover:text-white hover:bg-white/10 disabled:opacity-30"
                  onClick={(e) => {
                    e.stopPropagation();
                    setPageNumber((p) => Math.max(1, p - 1));
                  }}
                  disabled={pageNumber <= 1}
                  title="Previous Page"
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>

                <span className="text-[11px] sm:text-xs font-medium px-1 text-zinc-300 whitespace-nowrap">
                  {pageNumber} / {numPages}
                </span>

                <Button
                  variant="ghost"
                  size="icon"
                  className="h-7 w-7 rounded-full text-zinc-300 hover:text-white hover:bg-white/10 disabled:opacity-30"
                  onClick={(e) => {
                    e.stopPropagation();
                    setPageNumber((p) => Math.min(numPages, p + 1));
                  }}
                  disabled={pageNumber >= numPages}
                  title="Next Page"
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </>
          )}

          {/* Zoom Out */}
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7 sm:h-8 sm:w-8 rounded-full text-zinc-300 hover:text-white hover:bg-white/10"
            onClick={handleZoomOut}
            title="Zoom Out (-)"
          >
            <ZoomOut className="h-3.5 w-3.5 sm:h-4 sm:w-4" />
          </Button>

          {/* Zoom Level Indicator / Reset */}
          <button
            onClick={handleResetZoom}
            className="text-[10px] sm:text-xs font-semibold px-1.5 py-0.5 rounded text-zinc-300 hover:text-white hover:bg-white/10 transition-colors min-w-[38px] text-center"
            title="Reset Zoom to 100%"
          >
            {Math.round(zoomLevel * 100)}%
          </button>

          {/* Zoom In */}
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7 sm:h-8 sm:w-8 rounded-full text-zinc-300 hover:text-white hover:bg-white/10"
            onClick={handleZoomIn}
            title="Zoom In (+)"
          >
            <ZoomIn className="h-3.5 w-3.5 sm:h-4 sm:w-4" />
          </Button>

          {/* 90° Clockwise Rotate */}
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7 sm:h-8 sm:w-8 rounded-full text-zinc-300 hover:text-white hover:bg-white/10 ml-0.5"
            onClick={handleRotate}
            title="Rotate 90° (R)"
          >
            <RotateCw className="h-3.5 w-3.5 sm:h-4 sm:w-4" />
          </Button>

          {/* Mobile Document Switchers inside Dock (Mobile Only) */}
          {fileDocuments.length > 1 && (
            <div className="flex sm:hidden items-center gap-0.5 ml-1 pl-1.5 border-l border-white/15">
              <Button
                variant="ghost"
                size="icon"
                className="h-7 w-7 rounded-full text-zinc-300 hover:text-white disabled:opacity-30"
                onClick={handlePrevDoc}
                disabled={!hasPrevDoc}
                title="Previous Document"
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <Button
                variant="ghost"
                size="icon"
                className="h-7 w-7 rounded-full text-zinc-300 hover:text-white disabled:opacity-30"
                onClick={handleNextDoc}
                disabled={!hasNextDoc}
                title="Next Document"
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
