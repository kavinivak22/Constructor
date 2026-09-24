'use client';

import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { LucideIcon } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface Alert {
    id: string;
    icon: LucideIcon;
    title: string;
    description: string;
    time: string;
    variant: 'warning' | 'danger';
}

interface AlertFlipperProps {
    alerts: Alert[];
    alertVariants: Record<string, string>;
    autoplayDelay?: number;
}

function AlertCardBody({
    alert,
    variantClass,
}: {
    alert: Alert;
    variantClass: string;
}) {
    return (
        <div
            className={cn(
                "h-[120px] w-full flex items-center justify-between px-4 sm:px-6 select-none",
                variantClass
            )}
        >
            <div className="flex items-center gap-3 sm:gap-4 flex-1 min-w-0">
                <div className="flex-shrink-0 flex items-center justify-center">
                    <alert.icon className="h-5 w-5 sm:h-6 sm:w-6" />
                </div>
                <div className="flex-1 min-w-0">
                    <h4 className="font-semibold text-sm sm:font-bold sm:text-base truncate leading-snug">
                        {alert.title}
                    </h4>
                    <p className="text-xs sm:text-sm line-clamp-1 sm:line-clamp-2 leading-relaxed opacity-90">
                        {alert.description}
                    </p>
                    <p className="text-[10px] sm:text-xs opacity-75 mt-0.5">
                        {alert.time}
                    </p>
                </div>
            </div>
            <Button
                size="sm"
                variant="outline"
                className="text-current border-current/50 hover:bg-white/20 flex-shrink-0 h-8 sm:h-9 px-3 sm:px-4 text-xs sm:text-sm font-semibold rounded-lg ml-3"
            >
                View
            </Button>
        </div>
    );
}

export function AlertFlipper({
    alerts,
    alertVariants,
    autoplayDelay = 5000
}: AlertFlipperProps) {
    const [currentIndex, setCurrentIndex] = useState(0);
    const [nextIndex, setNextIndex] = useState(1);
    const [isFlipping, setIsFlipping] = useState(false);

    const currentAlert = alerts[currentIndex];
    const nextAlert = alerts[nextIndex];

    useEffect(() => {
        const timer = setInterval(() => {
            triggerFlip(nextIndex);
        }, autoplayDelay);

        return () => clearInterval(timer);
    }, [nextIndex, alerts.length, autoplayDelay]);

    const triggerFlip = (targetIndex: number) => {
        if (isFlipping) return;

        setIsFlipping(true);
        setNextIndex(targetIndex);

        // Wait for animation to complete (600ms matches CSS)
        setTimeout(() => {
            setCurrentIndex(targetIndex);
            setNextIndex((targetIndex + 1) % alerts.length);
            setIsFlipping(false);
        }, 600);
    };

    const handleDotClick = (index: number) => {
        if (!isFlipping && index !== currentIndex) {
            triggerFlip(index);
        }
    };

    return (
        <div className="w-full">
            {/* Split-Flap Display Container */}
            <div
                className="relative w-full overflow-visible"
                style={{
                    perspective: '2500px',
                    minHeight: '120px'
                }}
            >
                <div
                    className="relative rounded-xl overflow-hidden"
                    style={{
                        height: '120px',
                        boxShadow: '0 10px 30px rgba(0, 0, 0, 0.15), 0 4px 12px rgba(0, 0, 0, 0.1)'
                    }}
                >
                    {/* 
             LAYER 1: STATIC BOTTOM (Background)
             Shows Current Lower initially.
             After flip, this will be covered by the landed page.
          */}
                    <div
                        className="absolute left-0 right-0 overflow-hidden rounded-b-xl"
                        style={{
                            height: '60px',
                            top: '60px',
                            zIndex: 1,
                        }}
                    >
                        <div style={{ position: 'absolute', top: '-60px', left: 0, right: 0, height: '120px' }}>
                            <AlertCardBody alert={currentAlert} variantClass={alertVariants[currentAlert.variant]} />
                        </div>
                    </div>

                    {/* 
             LAYER 2: STATIC TOP (Background)
             Shows Next Upper.
             Revealed when the flipper moves away.
          */}
                    <div
                        className="absolute top-0 left-0 right-0 overflow-hidden rounded-t-xl"
                        style={{
                            height: '60px',
                            zIndex: 2,
                        }}
                    >
                        <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '120px' }}>
                            <AlertCardBody alert={nextAlert} variantClass={alertVariants[nextAlert.variant]} />
                        </div>
                    </div>

                    {/* 
             LAYER 3: FLIPPER (The Moving Page)
             Double-sided card that rotates.
             Front: Current Upper
             Back: Next Lower
          */}
                    <div
                        className={`absolute top-0 left-0 right-0 rounded-t-xl ${isFlipping ? 'flip-top-half' : ''
                            }`}
                        style={{
                            height: '60px',
                            transformStyle: 'preserve-3d',
                            transformOrigin: 'bottom center',
                            zIndex: 10,
                        }}
                    >
                        {/* FRONT FACE: Current Alert Upper */}
                        <div
                            className="absolute inset-0 backface-hidden overflow-hidden rounded-t-xl"
                            style={{ zIndex: 2 }}
                        >
                            <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '120px' }}>
                                <AlertCardBody alert={currentAlert} variantClass={alertVariants[currentAlert.variant]} />
                            </div>
                            {/* Shadow overlay for depth during flip */}
                            <div className={`absolute inset-0 bg-black/0 transition-colors duration-300 ${isFlipping ? 'animate-shadow-front' : ''}`} />
                        </div>

                        {/* BACK FACE: Next Alert Lower */}
                        <div
                            className="absolute inset-0 backface-hidden overflow-hidden rounded-b-xl"
                            style={{
                                transform: 'rotateX(180deg)',
                                zIndex: 1
                            }}
                        >
                            <div style={{ position: 'absolute', top: '-60px', left: 0, right: 0, height: '120px' }}>
                                <AlertCardBody alert={nextAlert} variantClass={alertVariants[nextAlert.variant]} />
                            </div>
                            {/* Highlight overlay for landing */}
                            <div className={`absolute inset-0 bg-white/0 transition-colors duration-300 ${isFlipping ? 'animate-shadow-back' : ''}`} />
                        </div>
                    </div>

                    {/* REFINED Center Line (Hinge) - Subtle and Thin */}
                    <div
                        className="absolute left-0 right-0 pointer-events-none z-20"
                        style={{
                            top: '60px',
                            height: '1px',
                            background: 'rgba(0,0,0,0.1)', // Very subtle line
                            boxShadow: '0 1px 2px rgba(0,0,0,0.05)', // Minimal shadow
                            transform: 'translateY(-0.5px)' // Centered exactly
                        }}
                    />
                </div>
            </div>

            {/* Progress Dots */}
            <div className="flex justify-center gap-2 mt-4">
                {alerts.map((_, index) => (
                    <button
                        key={index}
                        onClick={() => handleDotClick(index)}
                        className={`transition-all duration-300 rounded-full ${index === currentIndex
                                ? 'bg-primary w-8 h-2.5 shadow-md'
                                : 'bg-muted-foreground/30 hover:bg-muted-foreground/50 w-2.5 h-2.5'
                            }`}
                        aria-label={`Go to alert ${index + 1}`}
                    />
                ))}
            </div>
        </div>
    );
}
