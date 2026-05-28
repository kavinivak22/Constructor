'use client';

import { useState, useEffect } from 'react';
import { Progress } from '@/components/ui/progress';
import { X, CheckCircle2, FileText, Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';

interface UploadProgressPopupProps {
    progress: number;
    fileName: string;
    isVisible: boolean;
    onClose?: () => void;
    status: 'uploading' | 'completed' | 'error';
    errorMessage?: string;
}

export function UploadProgressPopup({
    progress,
    fileName,
    isVisible,
    onClose,
    status,
    errorMessage
}: UploadProgressPopupProps) {
    const [show, setShow] = useState(isVisible);

    useEffect(() => {
        if (isVisible) {
            setShow(true);
        } else {
            const timer = setTimeout(() => setShow(false), 500); // Wait for exit animation
            return () => clearTimeout(timer);
        }
    }, [isVisible]);

    useEffect(() => {
        if (status === 'completed') {
            const timer = setTimeout(() => {
                if (onClose) onClose();
            }, 3000); // Auto close after 3 seconds
            return () => clearTimeout(timer);
        }
    }, [status, onClose]);

    if (!show && !isVisible) return null;

    return (
        <div
            className={cn(
                "fixed top-4 right-4 z-[100] w-80 md:w-96 transition-all duration-500 ease-in-out transform",
                isVisible ? "translate-x-0 opacity-100" : "translate-x-full opacity-0"
            )}
        >
            <div className="bg-background/80 backdrop-blur-md border border-border/50 shadow-2xl rounded-xl overflow-hidden">
                {/* Header gradient line */}
                <div className={cn(
                    "h-1 w-full bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500",
                    status === 'error' && "from-red-500 via-red-500 to-red-500",
                    status === 'completed' && "from-green-500 via-emerald-500 to-green-500"
                )} />

                <div className="p-4">
                    <div className="flex items-start justify-between mb-3">
                        <div className="flex items-center gap-3 overflow-hidden">
                            <div className={cn(
                                "p-2 rounded-full bg-muted flex-shrink-0",
                                status === 'completed' && "bg-green-100 text-green-600 dark:bg-green-900/30",
                                status === 'error' && "bg-red-100 text-red-600 dark:bg-red-900/30"
                            )}>
                                {status === 'uploading' && <Loader2 className="h-5 w-5 animate-spin text-blue-500" />}
                                {status === 'completed' && <CheckCircle2 className="h-5 w-5" />}
                                {status === 'error' && <X className="h-5 w-5" />}
                            </div>
                            <div className="flex flex-col overflow-hidden">
                                <h4 className="text-sm font-semibold truncate">
                                    {status === 'uploading' ? 'Uploading...' :
                                        status === 'completed' ? 'Upload Complete' : 'Upload Failed'}
                                </h4>
                                <p className="text-xs text-muted-foreground truncate max-w-[180px]" title={fileName}>
                                    {fileName}
                                </p>
                            </div>
                        </div>
                        {onClose && (
                            <button
                                onClick={onClose}
                                className="text-muted-foreground hover:text-foreground transition-colors"
                            >
                                <X className="h-4 w-4" />
                            </button>
                        )}
                    </div>

                    {status !== 'error' && (
                        <div className="space-y-1">
                            <div className="flex justify-between text-xs text-muted-foreground">
                                <span>{Math.round(progress)}%</span>
                            </div>
                            <Progress value={progress} className="h-2" />
                        </div>
                    )}

                    {status === 'error' && errorMessage && (
                        <p className="text-xs text-red-500 mt-2">
                            {errorMessage}
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
}
