'use client';

import React, { useState, useEffect, useCallback } from 'react';
import Image from 'next/image';
import { X, ChevronLeft, ChevronRight, Maximize2, Calendar, Layers } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
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

  useEffect(() => {
    setCurrentIndex(initialIndex);
  }, [initialIndex, isOpen]);

  const handleNext = useCallback(() => {
    if (photos.length > 0) {
      setCurrentIndex((prev) => (prev + 1) % photos.length);
    }
  }, [photos.length]);

  const handlePrev = useCallback(() => {
    if (photos.length > 0) {
      setCurrentIndex((prev) => (prev - 1 + photos.length) % photos.length);
    }
  }, [photos.length]);

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!isOpen) return;
      if (e.key === 'ArrowRight') handleNext();
      if (e.key === 'ArrowLeft') handlePrev();
      if (e.key === 'Escape') onClose();
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, handleNext, handlePrev, onClose]);

  if (!isOpen || photos.length === 0) return null;

  const currentPhoto = photos[currentIndex];
  const photoUrl = currentPhoto?.photo_url || currentPhoto?.url || '';

  return (
    <Dialog open={isOpen} onOpenChange={(open) => { if (!open) onClose(); }}>
      <DialogContent className="fixed inset-0 z-50 w-screen h-screen max-w-none m-0 p-0 rounded-none bg-black/95 backdrop-blur-2xl border-none flex flex-col justify-between overflow-hidden outline-none">
        {/* Top Header Bar */}
        <div className="p-3 sm:p-4 bg-gradient-to-b from-black/80 to-transparent flex items-center justify-between z-30 shrink-0">
          <div className="flex items-center gap-2">
            <Badge variant="outline" className="bg-white/10 text-white border-white/20 text-xs font-semibold px-2.5 py-1">
              {currentIndex + 1} / {photos.length}
            </Badge>
            {dateLabel && (
              <Badge variant="outline" className="bg-white/10 text-white/90 border-white/15 text-[11px] font-medium px-2 py-0.5 hidden sm:flex items-center gap-1">
                <Calendar className="h-3 w-3 text-primary" /> {dateLabel}
              </Badge>
            )}
          </div>

          <Button
            size="icon"
            variant="ghost"
            onClick={onClose}
            className="h-10 w-10 rounded-full bg-white/10 text-white hover:bg-white/20 hover:text-white border border-white/15 transition-all"
            aria-label="Close fullscreen"
          >
            <X className="h-5 w-5" />
          </Button>
        </div>

        {/* Main Photo Center Container */}
        <div className="relative flex-1 flex items-center justify-center p-2 sm:p-6 min-h-0">
          {/* Previous Arrow Button */}
          {photos.length > 1 && (
            <Button
              size="icon"
              variant="ghost"
              onClick={handlePrev}
              className="absolute left-2 sm:left-6 top-1/2 -translate-y-1/2 z-30 h-10 w-10 sm:h-12 sm:w-12 rounded-full bg-black/50 text-white hover:bg-black/80 border border-white/20 backdrop-blur-md shadow-lg"
              aria-label="Previous image"
            >
              <ChevronLeft className="h-6 w-6" />
            </Button>
          )}

          {/* Photo Render */}
          <div className="relative w-full h-full flex items-center justify-center">
            {photoUrl ? (
              <Image
                src={photoUrl}
                alt={currentPhoto.caption || title}
                fill
                className="object-contain max-h-[80vh] max-w-[95vw] mx-auto select-none"
                priority
                unoptimized
              />
            ) : (
              <div className="text-white/60 text-sm">Image unavailable</div>
            )}
          </div>

          {/* Next Arrow Button */}
          {photos.length > 1 && (
            <Button
              size="icon"
              variant="ghost"
              onClick={handleNext}
              className="absolute right-2 sm:right-6 top-1/2 -translate-y-1/2 z-30 h-10 w-10 sm:h-12 sm:w-12 rounded-full bg-black/50 text-white hover:bg-black/80 border border-white/20 backdrop-blur-md shadow-lg"
              aria-label="Next image"
            >
              <ChevronRight className="h-6 w-6" />
            </Button>
          )}
        </div>

        {/* Bottom Footer Info Bar */}
        <div className="p-3 sm:p-5 bg-gradient-to-t from-black/90 via-black/60 to-transparent flex flex-col gap-2 z-30 shrink-0 text-center sm:text-left">
          {currentPhoto.caption ? (
            <p className="text-sm sm:text-base font-semibold text-white line-clamp-2 px-2">
              {currentPhoto.caption}
            </p>
          ) : (
            <p className="text-xs sm:text-sm font-medium text-white/80 line-clamp-1 px-2">
              {title}
            </p>
          )}

          {/* Mobile Bottom Swipe Navigation Bar */}
          {photos.length > 1 && (
            <div className="flex sm:hidden items-center justify-between gap-2 pt-1 border-t border-white/10 mt-1 px-2">
              <Button
                variant="outline"
                size="sm"
                onClick={handlePrev}
                className="h-8 text-xs font-semibold bg-white/10 text-white border-white/20 hover:bg-white/20 flex-1"
              >
                ← Previous
              </Button>
              <span className="text-[11px] text-white/60 font-medium px-1">
                {currentIndex + 1} of {photos.length}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={handleNext}
                className="h-8 text-xs font-semibold bg-white/10 text-white border-white/20 hover:bg-white/20 flex-1"
              >
                Next →
              </Button>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
