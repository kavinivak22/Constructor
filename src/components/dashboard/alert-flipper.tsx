'use client';

import React, { useEffect, useState, useRef, useCallback } from 'react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
    DialogFooter
} from '@/components/ui/dialog';
import {
    LucideIcon,
    AlertTriangle,
    ChevronLeft,
    ChevronRight,
    Sun,
    Wind,
    Users,
    CheckCircle2,
    Sparkles,
    RotateCcw,
    X,
    Eye,
    Pause,
    Play,
    CloudSun,
    Clock,
    ShieldCheck
} from 'lucide-react';
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

// -------------------------------------------------------------
// Split-Flap Card Body
// -------------------------------------------------------------
function AlertCardBody({
    alert,
    variantClass,
    onView,
}: {
    alert: Alert;
    variantClass: string;
    onView?: () => void;
}) {
    const IconComponent = alert.icon;
    return (
        <div
            className={cn(
                "h-[120px] w-full flex items-center justify-between px-4 sm:px-6 select-none cursor-pointer transition-colors",
                variantClass
            )}
            onClick={onView}
        >
            <div className="flex items-center gap-3 sm:gap-4 flex-1 min-w-0">
                <div className="flex-shrink-0 flex items-center justify-center p-2 rounded-xl bg-white/40 dark:bg-black/20 shadow-xs">
                    <IconComponent className="h-5 w-5 sm:h-6 sm:w-6" />
                </div>
                <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                        <h4 className="font-semibold text-sm sm:font-bold sm:text-base truncate leading-snug">
                            {alert.title}
                        </h4>
                        <span className="text-[10px] px-1.5 py-0.5 rounded-full uppercase tracking-wider font-semibold bg-black/10 dark:bg-white/10 shrink-0">
                            {alert.variant}
                        </span>
                    </div>
                    <p className="text-xs sm:text-sm line-clamp-1 sm:line-clamp-2 leading-relaxed opacity-90 mt-0.5">
                        {alert.description}
                    </p>
                    <p className="text-[10px] sm:text-xs opacity-75 mt-0.5 flex items-center gap-1">
                        <Clock className="w-3 h-3 inline" />
                        {alert.time}
                    </p>
                </div>
            </div>
            <Button
                size="sm"
                variant="outline"
                onClick={(e) => {
                    e.stopPropagation();
                    onView?.();
                }}
                className="text-current border-current/40 hover:bg-white/30 dark:hover:bg-black/30 flex-shrink-0 h-8 sm:h-9 px-3 sm:px-4 text-xs sm:text-sm font-semibold rounded-lg ml-3 shadow-xs"
            >
                View
            </Button>
        </div>
    );
}

// -------------------------------------------------------------
// Card-Free Living Creature Sentinel (Zero Alerts State)
// -------------------------------------------------------------
interface TelemetryItem {
    id: string;
    icon: LucideIcon;
    label: string;
    detail: string;
    status: 'optimal' | 'safe' | 'active';
    badgeColor: string;
    angleDeg: number; // orbital position in degrees
    distancePx: number; // mobile safe radial distance
}

const SITE_TELEMETRY: TelemetryItem[] = [
    {
        id: 'curing',
        icon: Sun,
        label: '31°C • Curing Optimal',
        detail: '0% Rain Risk • Slab pour window open until 6:30 PM',
        status: 'optimal',
        badgeColor: 'bg-emerald-500/15 text-emerald-700 dark:text-emerald-300 border-emerald-500/30',
        angleDeg: 210, // top-left
        distancePx: 82,
    },
    {
        id: 'wind',
        icon: Wind,
        label: '11 km/h • Crane Safe',
        detail: 'Calm breeze • Well below 38 km/h crane lifting limit',
        status: 'safe',
        badgeColor: 'bg-sky-500/15 text-sky-700 dark:text-sky-300 border-sky-500/30',
        angleDeg: 330, // top-right
        distancePx: 82,
    },
    {
        id: 'labor',
        icon: Users,
        label: '18 Active On-Site',
        detail: '3 active project sites • All check-ins accounted for',
        status: 'active',
        badgeColor: 'bg-amber-500/15 text-amber-700 dark:text-amber-300 border-amber-500/30',
        angleDeg: 90, // bottom-center
        distancePx: 74,
    },
];

function LivingCreatureSentinel({
    onRestoreAlerts,
}: {
    onRestoreAlerts: () => void;
}) {
    const [pupilOffset, setPupilOffset] = useState({ x: 0, y: 0 });
    const [isBlinking, setIsBlinking] = useState(false);
    const [isBloomed, setIsBloomed] = useState(false);
    const [isSquished, setIsSquished] = useState(false);
    const [selectedTelemetry, setSelectedTelemetry] = useState<TelemetryItem | null>(null);
    const containerRef = useRef<HTMLDivElement>(null);

    // Natural autonomous glancing when idle
    useEffect(() => {
        const glanceInterval = setInterval(() => {
            if (Math.random() > 0.4) {
                const randomX = (Math.random() - 0.5) * 6;
                const randomY = (Math.random() - 0.5) * 4;
                setPupilOffset({ x: randomX, y: randomY });
                setTimeout(() => {
                    setPupilOffset({ x: 0, y: 0 });
                }, 1200);
            }
        }, 3200);

        return () => clearInterval(glanceInterval);
    }, []);

    // Natural blinking
    useEffect(() => {
        const blinkInterval = setInterval(() => {
            setIsBlinking(true);
            setTimeout(() => setIsBlinking(false), 180);
        }, 3800);

        return () => clearInterval(blinkInterval);
    }, []);

    // Pointer & Touch Tracking (Eyes follow mouse or finger)
    const handlePointerMove = useCallback((e: React.PointerEvent<HTMLDivElement>) => {
        if (!containerRef.current) return;
        const rect = containerRef.current.getBoundingClientRect();
        const centerX = rect.left + rect.width / 2;
        const centerY = rect.top + rect.height / 2;

        const deltaX = (e.clientX - centerX) / (rect.width / 2);
        const deltaY = (e.clientY - centerY) / (rect.height / 2);

        const clampedX = Math.max(-6, Math.min(6, deltaX * 6));
        const clampedY = Math.max(-5, Math.min(5, deltaY * 5));

        setPupilOffset({ x: clampedX, y: clampedY });
    }, []);

    const handlePointerLeave = useCallback(() => {
        setPupilOffset({ x: 0, y: 0 });
    }, []);

    const handleCreatureTap = () => {
        setIsSquished(true);
        setTimeout(() => setIsSquished(false), 320);
        setIsBloomed((prev) => !prev);
        setSelectedTelemetry(null);
    };

    return (
        <div
            ref={containerRef}
            onPointerMove={handlePointerMove}
            onPointerLeave={handlePointerLeave}
            className="relative w-full flex flex-col items-center justify-center py-3 select-none touch-none bg-transparent"
            style={{ minHeight: '136px' }}
        >
            {/* Ambient soft luminous aura (No cards, purely organic light) */}
            <div
                className="absolute pointer-events-none rounded-full blur-2xl transition-all duration-700 ease-out"
                style={{
                    width: isBloomed ? '240px' : '150px',
                    height: isBloomed ? '140px' : '110px',
                    background: isBloomed
                        ? 'radial-gradient(circle, rgba(249, 115, 22, 0.22) 0%, rgba(245, 158, 11, 0.12) 45%, rgba(0,0,0,0) 75%)'
                        : 'radial-gradient(circle, rgba(249, 115, 22, 0.14) 0%, rgba(245, 158, 11, 0.06) 50%, rgba(0,0,0,0) 80%)',
                }}
            />

            {/* Orbiting / Blooming Telemetry Petals (Useful Construction Intelligence) */}
            {SITE_TELEMETRY.map((item) => {
                const angleRad = (item.angleDeg * Math.PI) / 180;
                const tx = isBloomed ? Math.cos(angleRad) * item.distancePx : 0;
                const ty = isBloomed ? Math.sin(angleRad) * item.distancePx : 0;
                const IconComponent = item.icon;
                const isSelected = selectedTelemetry?.id === item.id;

                return (
                    <div
                        key={item.id}
                        onClick={(e) => {
                            e.stopPropagation();
                            setSelectedTelemetry((prev) => (prev?.id === item.id ? null : item));
                        }}
                        style={{
                            transform: `translate(${tx}px, ${ty}px) scale(${isBloomed ? 1 : 0.4})`,
                            opacity: isBloomed ? 1 : 0,
                            pointerEvents: isBloomed ? 'auto' : 'none',
                        }}
                        className={cn(
                            "absolute z-20 flex items-center gap-1.5 px-2.5 py-1.5 rounded-full text-xs font-medium cursor-pointer transition-all duration-500 ease-out shadow-md backdrop-blur-md border",
                            item.badgeColor,
                            isSelected ? "ring-2 ring-primary ring-offset-1 scale-105" : "hover:scale-105 active:scale-95"
                        )}
                    >
                        <IconComponent className="w-3.5 h-3.5 shrink-0" />
                        <span className="text-[11px] whitespace-nowrap font-semibold tracking-tight">
                            {item.label}
                        </span>
                    </div>
                );
            })}

            {/* Living Creature Character Body */}
            <div
                onClick={handleCreatureTap}
                className="relative z-10 cursor-pointer transition-transform duration-300 ease-out active:scale-90"
                style={{
                    transform: isSquished
                        ? 'scale(1.22, 0.78)'
                        : isBloomed
                        ? 'scale(1.05)'
                        : 'scale(1)',
                }}
            >
                <svg
                    width="96"
                    height="96"
                    viewBox="0 0 100 100"
                    className="filter drop-shadow-[0_8px_16px_rgba(249,115,22,0.28)] transition-all duration-300"
                >
                    <defs>
                        <linearGradient id="creatureGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                            <stop offset="0%" stopColor="#f97316" />
                            <stop offset="60%" stopColor="#fb923c" />
                            <stop offset="100%" stopColor="#f59e0b" />
                        </linearGradient>
                        <radialGradient id="creatureHighlight" cx="35%" cy="30%" r="60%">
                            <stop offset="0%" stopColor="rgba(255,255,255,0.7)" />
                            <stop offset="50%" stopColor="rgba(255,255,255,0.15)" />
                            <stop offset="100%" stopColor="rgba(255,255,255,0)" />
                        </radialGradient>
                    </defs>

                    {/* Organic Body Blob with subtle morphing curves */}
                    <path
                        d="M 50 14 C 74 14, 88 28, 88 50 C 88 74, 72 86, 50 86 C 28 86, 12 74, 12 50 C 12 28, 26 14, 50 14 Z"
                        fill="url(#creatureGrad)"
                    />
                    {/* Soft 3D lighting reflection */}
                    <path
                        d="M 50 14 C 74 14, 88 28, 88 50 C 88 74, 72 86, 50 86 C 28 86, 12 74, 12 50 C 12 28, 26 14, 50 14 Z"
                        fill="url(#creatureHighlight)"
                    />

                    {/* Left Eye */}
                    {isBloomed ? (
                        // Happy curved eye when bloomed
                        <path
                            d="M 33 48 Q 38 41 43 48"
                            stroke="#ffffff"
                            strokeWidth="3.2"
                            strokeLinecap="round"
                            fill="none"
                        />
                    ) : isBlinking ? (
                        // Blinking slit
                        <line
                            x1="33"
                            y1="46"
                            x2="43"
                            y2="46"
                            stroke="#ffffff"
                            strokeWidth="2.8"
                            strokeLinecap="round"
                        />
                    ) : (
                        // Sclera + Iris
                        <g>
                            <circle cx="38" cy="46" r="7" fill="#ffffff" />
                            <circle
                                cx={38 + pupilOffset.x}
                                cy={46 + pupilOffset.y}
                                r="4"
                                fill="#1e293b"
                            />
                            {/* Eye sparkle reflection */}
                            <circle
                                cx={38 + pupilOffset.x + 1.2}
                                cy={46 + pupilOffset.y - 1.2}
                                r="1.3"
                                fill="#ffffff"
                            />
                        </g>
                    )}

                    {/* Right Eye */}
                    {isBloomed ? (
                        // Happy curved eye when bloomed
                        <path
                            d="M 57 48 Q 62 41 67 48"
                            stroke="#ffffff"
                            strokeWidth="3.2"
                            strokeLinecap="round"
                            fill="none"
                        />
                    ) : isBlinking ? (
                        // Blinking slit
                        <line
                            x1="57"
                            y1="46"
                            x2="67"
                            y2="46"
                            stroke="#ffffff"
                            strokeWidth="2.8"
                            strokeLinecap="round"
                        />
                    ) : (
                        // Sclera + Iris
                        <g>
                            <circle cx="62" cy="46" r="7" fill="#ffffff" />
                            <circle
                                cx={62 + pupilOffset.x}
                                cy={46 + pupilOffset.y}
                                r="4"
                                fill="#1e293b"
                            />
                            {/* Eye sparkle reflection */}
                            <circle
                                cx={62 + pupilOffset.x + 1.2}
                                cy={46 + pupilOffset.y - 1.2}
                                r="1.3"
                                fill="#ffffff"
                            />
                        </g>
                    )}

                    {/* Cute Smile */}
                    <path
                        d={
                            isBloomed
                                ? "M 44 57 Q 50 65 56 57" // Wide happy grin
                                : "M 46 56 Q 50 60 54 56" // Gentle curious smile
                        }
                        stroke="#ffffff"
                        strokeWidth="2.4"
                        strokeLinecap="round"
                        fill="none"
                    />

                    {/* Cheeks (Blush) */}
                    <ellipse cx="30" cy="53" rx="3.5" ry="2" fill="rgba(255,255,255,0.3)" />
                    <ellipse cx="70" cy="53" rx="3.5" ry="2" fill="rgba(255,255,255,0.3)" />
                </svg>
            </div>

            {/* Telemetry Detail Popover (Shown when a bloomed petal is tapped) */}
            {selectedTelemetry && (
                <div className="absolute top-[96%] z-30 flex items-center gap-2 px-3 py-1.5 rounded-xl bg-background/90 dark:bg-card/90 shadow-lg border border-border/60 text-xs backdrop-blur-md animate-in fade-in zoom-in-95 duration-200">
                    <ShieldCheck className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                    <span className="text-[11px] text-foreground font-medium">
                        {selectedTelemetry.detail}
                    </span>
                    <button
                        onClick={(e) => {
                            e.stopPropagation();
                            setSelectedTelemetry(null);
                        }}
                        className="text-muted-foreground hover:text-foreground ml-1"
                    >
                        <X className="w-3 h-3" />
                    </button>
                </div>
            )}

            {/* Subtle Restore Alerts button (Discrete simulation control) */}
            <button
                onClick={onRestoreAlerts}
                title="Restore dashboard alerts preview"
                className="absolute right-2 top-2 p-1.5 rounded-full text-muted-foreground/40 hover:text-muted-foreground hover:bg-muted/30 transition-all text-[11px] flex items-center gap-1"
            >
                <RotateCcw className="w-3.5 h-3.5" />
                <span className="text-[10px] hidden sm:inline">Simulate Alerts</span>
            </button>
        </div>
    );
}

// -------------------------------------------------------------
// Main Alert Flipper Component
// -------------------------------------------------------------
export function AlertFlipper({
    alerts: initialAlerts,
    alertVariants,
    autoplayDelay = 5000,
}: AlertFlipperProps) {
    const [alertsList, setAlertsList] = useState<Alert[]>(initialAlerts);
    const [currentIndex, setCurrentIndex] = useState(0);
    const [nextIndex, setNextIndex] = useState(1);
    const [isFlipping, setIsFlipping] = useState(false);
    const [isPaused, setIsPaused] = useState(false);
    const [inspectingAlert, setInspectingAlert] = useState<Alert | null>(null);

    // Sync if initialAlerts prop changes
    useEffect(() => {
        setAlertsList(initialAlerts);
    }, [initialAlerts]);

    const activeCount = alertsList.length;

    // Automatic flip timer (pauses when user hovers or touches)
    useEffect(() => {
        if (activeCount <= 1 || isPaused || isFlipping) return;

        const timer = setInterval(() => {
            triggerFlip((currentIndex + 1) % activeCount);
        }, autoplayDelay);

        return () => clearInterval(timer);
    }, [currentIndex, activeCount, isPaused, isFlipping, autoplayDelay]);

    const triggerFlip = (targetIndex: number) => {
        if (isFlipping || activeCount <= 1) return;

        setIsFlipping(true);
        setNextIndex(targetIndex);

        // Wait for animation to complete (600ms matches CSS)
        setTimeout(() => {
            setCurrentIndex(targetIndex);
            setNextIndex((targetIndex + 1) % activeCount);
            setIsFlipping(false);
        }, 600);
    };

    const handlePrev = (e: React.MouseEvent) => {
        e.stopPropagation();
        if (activeCount <= 1 || isFlipping) return;
        const prev = (currentIndex - 1 + activeCount) % activeCount;
        triggerFlip(prev);
    };

    const handleNext = (e: React.MouseEvent) => {
        e.stopPropagation();
        if (activeCount <= 1 || isFlipping) return;
        const next = (currentIndex + 1) % activeCount;
        triggerFlip(next);
    };

    const handleDismissAlert = (alertId: string) => {
        const nextAlerts = alertsList.filter((a) => a.id !== alertId);
        setAlertsList(nextAlerts);
        setInspectingAlert(null);
        if (nextAlerts.length > 0) {
            setCurrentIndex((prev) => (prev >= nextAlerts.length ? 0 : prev));
            setNextIndex(0);
        }
    };

    const handleRestoreAlerts = () => {
        setAlertsList(initialAlerts);
        setCurrentIndex(0);
        setNextIndex(initialAlerts.length > 1 ? 1 : 0);
    };

    // If zero active alerts: Render the minimalist, card-free Living Creature Widget
    if (activeCount === 0) {
        return (
            <LivingCreatureSentinel onRestoreAlerts={handleRestoreAlerts} />
        );
    }

    const currentAlert = alertsList[currentIndex] || alertsList[0];
    const safeNextIndex = nextIndex < activeCount ? nextIndex : 0;
    const nextAlert = alertsList[safeNextIndex] || currentAlert;

    return (
        <div
            className="w-full relative group"
            onMouseEnter={() => setIsPaused(true)}
            onMouseLeave={() => setIsPaused(false)}
            onTouchStart={() => setIsPaused(true)}
            onTouchEnd={() => setIsPaused(false)}
        >
            {/* Split-Flap Display Container */}
            <div
                className="relative w-full overflow-visible"
                style={{
                    perspective: '2500px',
                    minHeight: '120px',
                }}
            >
                <div
                    className="relative rounded-xl overflow-hidden cursor-pointer"
                    style={{
                        height: '120px',
                        boxShadow: '0 10px 30px rgba(0, 0, 0, 0.12), 0 4px 12px rgba(0, 0, 0, 0.08)',
                    }}
                    onClick={() => setInspectingAlert(currentAlert)}
                >
                    {/* LAYER 1: STATIC BOTTOM (Background) */}
                    <div
                        className="absolute left-0 right-0 overflow-hidden rounded-b-xl"
                        style={{
                            height: '60px',
                            top: '60px',
                            zIndex: 1,
                        }}
                    >
                        <div style={{ position: 'absolute', top: '-60px', left: 0, right: 0, height: '120px' }}>
                            <AlertCardBody
                                alert={currentAlert}
                                variantClass={alertVariants[currentAlert.variant]}
                                onView={() => setInspectingAlert(currentAlert)}
                            />
                        </div>
                    </div>

                    {/* LAYER 2: STATIC TOP (Background) */}
                    <div
                        className="absolute top-0 left-0 right-0 overflow-hidden rounded-t-xl"
                        style={{
                            height: '60px',
                            zIndex: 2,
                        }}
                    >
                        <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '120px' }}>
                            <AlertCardBody
                                alert={nextAlert}
                                variantClass={alertVariants[nextAlert.variant]}
                                onView={() => setInspectingAlert(nextAlert)}
                            />
                        </div>
                    </div>

                    {/* LAYER 3: FLIPPER (The Moving Page) */}
                    <div
                        className={`absolute top-0 left-0 right-0 rounded-t-xl ${
                            isFlipping ? 'flip-top-half' : ''
                        }`}
                        style={{
                            height: '60px',
                            transformStyle: 'preserve-3d',
                            transformOrigin: 'bottom center',
                            zIndex: 10,
                        }}
                    >
                        {/* FRONT FACE */}
                        <div
                            className="absolute inset-0 backface-hidden overflow-hidden rounded-t-xl"
                            style={{ zIndex: 2 }}
                        >
                            <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '120px' }}>
                                <AlertCardBody
                                    alert={currentAlert}
                                    variantClass={alertVariants[currentAlert.variant]}
                                    onView={() => setInspectingAlert(currentAlert)}
                                />
                            </div>
                            <div
                                className={`absolute inset-0 bg-black/0 transition-colors duration-300 ${
                                    isFlipping ? 'animate-shadow-front' : ''
                                }`}
                            />
                        </div>

                        {/* BACK FACE */}
                        <div
                            className="absolute inset-0 backface-hidden overflow-hidden rounded-b-xl"
                            style={{
                                transform: 'rotateX(180deg)',
                                zIndex: 1,
                            }}
                        >
                            <div style={{ position: 'absolute', top: '-60px', left: 0, right: 0, height: '120px' }}>
                                <AlertCardBody
                                    alert={nextAlert}
                                    variantClass={alertVariants[nextAlert.variant]}
                                    onView={() => setInspectingAlert(nextAlert)}
                                />
                            </div>
                            <div
                                className={`absolute inset-0 bg-white/0 transition-colors duration-300 ${
                                    isFlipping ? 'animate-shadow-back' : ''
                                }`}
                            />
                        </div>
                    </div>

                    {/* REFINED Center Line (Hinge) */}
                    <div
                        className="absolute left-0 right-0 pointer-events-none z-20"
                        style={{
                            top: '60px',
                            height: '1px',
                            background: 'rgba(0,0,0,0.12)',
                            boxShadow: '0 1px 2px rgba(0,0,0,0.06)',
                            transform: 'translateY(-0.5px)',
                        }}
                    />

                    {/* Manual Navigation Arrows (Visible on hover and mobile) */}
                    {activeCount > 1 && (
                        <>
                            <button
                                onClick={handlePrev}
                                aria-label="Previous alert"
                                className="absolute left-2 top-1/2 -translate-y-1/2 z-30 p-1.5 rounded-full bg-black/20 hover:bg-black/40 text-white backdrop-blur-xs transition-opacity duration-200 opacity-0 group-hover:opacity-100 sm:opacity-0 focus:opacity-100"
                            >
                                <ChevronLeft className="w-4 h-4" />
                            </button>
                            <button
                                onClick={handleNext}
                                aria-label="Next alert"
                                className="absolute right-2 top-1/2 -translate-y-1/2 z-30 p-1.5 rounded-full bg-black/20 hover:bg-black/40 text-white backdrop-blur-xs transition-opacity duration-200 opacity-0 group-hover:opacity-100 sm:opacity-0 focus:opacity-100"
                            >
                                <ChevronRight className="w-4 h-4" />
                            </button>
                        </>
                    )}
                </div>
            </div>

            {/* Bottom Controls Bar: Progress Dots, Pause Status, and View Creature Button */}
            <div className="flex items-center justify-between mt-3 px-1">
                {/* Status indicator */}
                <div className="flex items-center gap-1.5 text-[11px] text-muted-foreground">
                    {isPaused ? (
                        <>
                            <Pause className="w-3 h-3 text-primary animate-pulse" />
                            <span>Paused</span>
                        </>
                    ) : (
                        <span>
                            {currentIndex + 1} of {activeCount}
                        </span>
                    )}
                </div>

                {/* Progress Dots */}
                <div className="flex items-center gap-1.5">
                    {alertsList.map((_, index) => (
                        <button
                            key={index}
                            onClick={() => triggerFlip(index)}
                            className={`transition-all duration-300 rounded-full ${
                                index === currentIndex
                                    ? 'bg-primary w-6 h-2 shadow-xs'
                                    : 'bg-muted-foreground/30 hover:bg-muted-foreground/50 w-2 h-2'
                            }`}
                            aria-label={`Go to alert ${index + 1}`}
                        />
                    ))}
                </div>

                {/* Switch to Creature Preview */}
                <button
                    onClick={() => setAlertsList([])}
                    title="Clear alerts to see living creature"
                    className="flex items-center gap-1 text-[11px] text-muted-foreground/70 hover:text-primary transition-colors"
                >
                    <Sparkles className="w-3 h-3" />
                    <span className="hidden sm:inline">Living Creature</span>
                </button>
            </div>

            {/* Alert Inspection & Dismissal Dialog */}
            {inspectingAlert && (
                <Dialog open={!!inspectingAlert} onOpenChange={(open) => !open && setInspectingAlert(null)}>
                    <DialogContent className="sm:max-w-md">
                        <DialogHeader>
                            <div className="flex items-center gap-2 mb-1">
                                <Badge
                                    variant={inspectingAlert.variant === 'danger' ? 'destructive' : 'outline'}
                                    className="uppercase tracking-wider text-[10px]"
                                >
                                    {inspectingAlert.variant}
                                </Badge>
                                <span className="text-xs text-muted-foreground flex items-center gap-1">
                                    <Clock className="w-3 h-3 inline" />
                                    {inspectingAlert.time}
                                </span>
                            </div>
                            <DialogTitle className="text-base sm:text-lg flex items-center gap-2">
                                <inspectingAlert.icon className="w-5 h-5 text-primary shrink-0" />
                                <span>{inspectingAlert.title}</span>
                            </DialogTitle>
                            <DialogDescription className="text-sm pt-2 leading-relaxed text-foreground/90">
                                {inspectingAlert.description}
                            </DialogDescription>
                        </DialogHeader>

                        <div className="p-3 rounded-xl bg-muted/40 border text-xs text-muted-foreground flex items-center gap-2">
                            <ShieldCheck className="w-4 h-4 text-primary shrink-0" />
                            <span>Actionable reminder logged in Constructor site registry.</span>
                        </div>

                        <DialogFooter className="flex-row items-center justify-between sm:justify-end gap-2 pt-2">
                            <Button
                                type="button"
                                variant="destructive"
                                size="sm"
                                onClick={() => handleDismissAlert(inspectingAlert.id)}
                                className="text-xs"
                            >
                                Dismiss Alert
                            </Button>
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => setInspectingAlert(null)}
                                className="text-xs"
                            >
                                Got it
                            </Button>
                        </DialogFooter>
                    </DialogContent>
                </Dialog>
            )}
        </div>
    );
}
