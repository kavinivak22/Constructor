'use client';

import React, { useState, useEffect, useCallback, useRef } from 'react';
import Image from 'next/image';
import { X, ChevronLeft, ChevronRight, ZoomIn, ZoomOut, RotateCcw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent } from '@/components/ui/dialog';

interface FullscreenPhotoViewerProps {
  photos: Array<{ photo_url?: string; url?: string; caption?: string }>;
  initialIndex?: number;
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  dateLabel?: string;
}

export function FullscreenPhotoViewer({
  photos,
  initialIndex = 0,
  isOpen,
  onClose,
  title = 'Site Photo',
  dateLabel,
}: FullscreenPhotoViewerProps) {
  const [currentIndex, setCurrentIndex] = useState(initialIndex);
  const [scale, setScale] = useState(1);
  const [position, setPosition] = useState({ x: 0, y: 0 });

  // Touch tracking refs
  const initialTouchDistRef = useRef<number | null>(null);
  const initialScaleRef = useRef<number>(1);
  const touchStartRef = useRef<{ x: number; y: number }>({ x: 0, y: 0 });
  const positionStartRef = useRef<{ x: number; y: number }>({ x: 0, y: 0 });
  const lastTapRef = useRef<number>(0);
  const swipeStartXRef = useRef<number | null>(null);

  const resetZoom = useCallback(() => {
    setScale(1);
    setPosition({ x: 0, y: 0 });
  }, []);

  useEffect(() => {
    setCurrentIndex(initialIndex);
    resetZoom();
  }, [initialIndex, isOpen, resetZoom]);

  const handleNext = useCallback(() => {
    if (photos.length > 0) {
      resetZoom();
      setCurrentIndex((prev) => (prev + 1) % photos.length);
    }
  }, [photos.length, resetZoom]);

  const handlePrev = useCallback(() => {
    if (photos.length > 0) {
      resetZoom();
      setCurrentIndex((prev) => (prev - 1 + photos.length) % photos.length);
    }
  }, [photos.length, resetZoom]);

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!isOpen) return;
      if (e.key === 'ArrowRight') handleNext();
      if (e.key === 'ArrowLeft') handlePrev();
      if (e.key === 'Escape') onClose();
      if (e.key === '+' || e.key === '=') setScale((s) => Math.min(s + 0.5, 4));
      if (e.key === '-') setScale((s) => Math.max(s - 0.5, 1));
      if (e.key === '0') resetZoom();
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, handleNext, handlePrev, onClose, resetZoom]);

  // Double tap / Double click to zoom
  const handleDoubleTap = (e: React.MouseEvent | React.TouchEvent) => {
    const now = Date.now();
    if (now - lastTapRef.current < 300) {
      if (scale > 1) {
        resetZoom();
      } else {
        setScale(2.5);
      }
    }
    lastTapRef.current = now;
  };

  // Touch events for Pinch-to-Zoom, Panning & Swiping
  const handleTouchStart = (e: React.TouchEvent) => {
    if (e.touches.length === 2) {
      // Pinch start
      const dist = Math.hypot(
        e.touches[0].clientX - e.touches[1].clientX,
        e.touches[0].clientY - e.touches[1].clientY
      );
      initialTouchDistRef.current = dist;
      initialScaleRef.current = scale;
    } else if (e.touches.length === 1) {
      handleDoubleTap(e);
      swipeStartXRef.current = e.touches[0].clientX;
      touchStartRef.current = { x: e.touches[0].clientX, y: e.touches[0].clientY };
      positionStartRef.current = { ...position };
    }
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (e.touches.length === 2 && initialTouchDistRef.current !== null) {
      // Pinching
      const dist = Math.hypot(
        e.touches[0].clientX - e.touches[1].clientX,
        e.touches[0].clientY - e.touches[1].clientY
      );
      const newScale = Math.min(
        Math.max(initialScaleRef.current * (dist / initialTouchDistRef.current), 1),
        4
      );
      setScale(newScale);
      if (newScale === 1) setPosition({ x: 0, y: 0 });
    } else if (e.touches.length === 1 && scale > 1) {
      // Panning when zoomed in
      const deltaX = e.touches[0].clientX - touchStartRef.current.x;
      const deltaY = e.touches[0].clientY - touchStartRef.current.y;
      setPosition({
        x: positionStartRef.current.x + deltaX,
        y: positionStartRef.current.y + deltaY,
      });
    }
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (e.touches.length < 2) {
      initialTouchDistRef.current = null;
    }
    if (scale === 1 && swipeStartXRef.current !== null && e.changedTouches.length > 0) {
      const deltaX = e.changedTouches[0].clientX - swipeStartXRef.current;
      if (deltaX < -60) handleNext();
      if (deltaX > 60) handlePrev();
    }
    swipeStartXRef.current = null;
  };

  if (!isOpen || photos.length === 0) return null;

  const currentPhoto = photos[currentIndex];
  const photoUrl = currentPhoto?.photo_url || currentPhoto?.url || '';

  return (
    <Dialog open={isOpen} onOpenChange={(open) => { if (!open) onClose(); }}>
      <DialogContent className="fixed inset-0 z-[100] w-screen h-screen max-w-none m-0 p-0 rounded-none bg-black border-none flex flex-col justify-between overflow-hidden outline-none select-none">
        {/* Minimalist Top Header Bar */}
        <div className="absolute top-0 left-0 right-0 p-4 bg-gradient-to-b from-black/80 via-black/40 to-transparent flex items-center justify-between z-40 pointer-events-auto">
          <div className="flex items-center gap-2">
            <span className="bg-white/10 backdrop-blur-md text-white/90 border border-white/15 text-xs font-mono px-3 py-1 rounded-full">
              {currentIndex + 1} / {photos.length}
            </span>
            {scale > 1 && (
              <span className="bg-amber-500/20 text-amber-300 border border-amber-500/30 text-[10px] font-semibold px-2 py-0.5 rounded-full flex items-center gap-1">
                {scale.toFixed(1)}x Zoom
              </span>
            )}
          </div>

          <div className="flex items-center gap-2">
            {scale > 1 && (
              <Button
                size="icon"
                variant="ghost"
                onClick={resetZoom}
                className="h-9 w-9 rounded-full bg-white/10 text-white/80 hover:bg-white/20 hover:text-white backdrop-blur-md border border-white/10"
                title="Reset Zoom"
              >
                <RotateCcw className="h-4 w-4" />
              </Button>
            )}
            <Button
              size="icon"
              variant="ghost"
              onClick={onClose}
              className="h-9 w-9 rounded-full bg-white/15 text-white hover:bg-white/30 backdrop-blur-md border border-white/20"
              aria-label="Close fullscreen"
            >
              <X className="h-4.5 w-4.5" />
            </Button>
          </div>
        </div>

        {/* Center Canvas Area with Touch Gesture Handlers */}
        <div
          className="relative flex-1 flex items-center justify-center overflow-hidden touch-none"
          onTouchStart={handleTouchStart}
          onTouchMove={handleTouchMove}
          onTouchEnd={handleTouchEnd}
          onDoubleClick={handleDoubleTap}
        >
          {/* Desktop Left Chevron Arrow */}
          {photos.length > 1 && (
            <Button
              size="icon"
              variant="ghost"
              onClick={(e) => { e.stopPropagation(); handlePrev(); }}
              className="hidden sm:flex absolute left-4 top-1/2 -translate-y-1/2 z-40 h-11 w-11 rounded-full bg-black/40 text-white/80 hover:bg-black/70 hover:text-white border border-white/15 backdrop-blur-md transition-all opacity-60 hover:opacity-100"
              aria-label="Previous photo"
            >
              <ChevronLeft className="h-6 w-6" />
            </Button>
          )}

          {/* Scalable & Pannable Image Container */}
          <div
            className="relative w-full h-full flex items-center justify-center transition-transform duration-75 ease-out"
            style={{
              transform: `translate3d(${position.x}px, ${position.y}px, 0px) scale(${scale})`,
              transformOrigin: 'center center',
            }}
          >
            {photoUrl ? (
              <Image
                src={photoUrl}
                alt={currentPhoto.caption || title}
                fill
                className="object-contain max-h-[90vh] max-w-[95vw] mx-auto select-none pointer-events-none"
                priority
                unoptimized
              />
            ) : (
              <div className="text-white/60 text-sm font-medium">Image unavailable</div>
            )}
          </div>

          {/* Desktop Right Chevron Arrow */}
          {photos.length > 1 && (
            <Button
              size="icon"
              variant="ghost"
              onClick={(e) => { e.stopPropagation(); handleNext(); }}
              className="hidden sm:flex absolute right-4 top-1/2 -translate-y-1/2 z-40 h-11 w-11 rounded-full bg-black/40 text-white/80 hover:bg-black/70 hover:text-white border border-white/15 backdrop-blur-md transition-all opacity-60 hover:opacity-100"
              aria-label="Next photo"
            >
              <ChevronRight className="h-6 w-6" />
            </Button>
          )}
        </div>

        {/* Minimalist Bottom Caption Bar */}
        {(currentPhoto.caption || title || dateLabel) && (
          <div className="absolute bottom-0 left-0 right-0 p-4 pb-6 bg-gradient-to-t from-black/90 via-black/50 to-transparent z-40 pointer-events-none text-center sm:text-left">
            <div className="max-w-2xl mx-auto space-y-1">
              <p className="text-sm sm:text-base font-medium text-white/95 line-clamp-2 px-2 drop-shadow-md">
                {currentPhoto.caption || title}
              </p>
              {dateLabel && (
                <p className="text-[11px] font-mono text-white/60 px-2">
                  {dateLabel}
                </p>
              )}
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
