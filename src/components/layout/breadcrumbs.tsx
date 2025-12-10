'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { ChevronRight, Home } from 'lucide-react';
import { Fragment, useEffect, useState } from 'react';
import { useSupabase } from '@/supabase/provider';

export function Breadcrumbs() {
    const pathname = usePathname();
    const { supabase } = useSupabase();
    const [segmentLabels, setSegmentLabels] = useState<Record<string, string>>({});

    // Remove query parameters and split by slash
    const pathSegments = pathname
        .split('?')[0]
        .split('/')
        .filter((segment) => segment !== '');

    useEffect(() => {
        const fetchSegmentLabels = async () => {
            const newLabels: Record<string, string> = {};

            for (let i = 0; i < pathSegments.length; i++) {
                const segment = pathSegments[i];
                const prevSegment = i > 0 ? pathSegments[i - 1] : null;

                // Check if we already have a label for this segment to avoid re-fetching
                if (segmentLabels[segment]) continue;

                if (prevSegment === 'projects' && segment !== 'create') {
                    // This is likely a project ID
                    const { data } = await supabase
                        .from('projects')
                        .select('name')
                        .eq('id', segment)
                        .single();

                    if (data) {
                        newLabels[segment] = data.name;
                    }
                }
                // Add more conditions here for other resources like employees, etc.
            }

            if (Object.keys(newLabels).length > 0) {
                setSegmentLabels(prev => ({ ...prev, ...newLabels }));
            }
        };

        fetchSegmentLabels();
    }, [pathname, supabase]); // Re-run when pathname changes

    const breadcrumbs = pathSegments.map((segment, index) => {
        const href = `/${pathSegments.slice(0, index + 1).join('/')}`;

        // Use fetched label if available, otherwise format the segment
        let label = segmentLabels[segment];

        if (!label) {
            label = segment
                .replace(/[-_]/g, ' ')
                .replace(/\b\w/g, (char) => char.toUpperCase());
        }

        return {
            href,
            label,
            isLast: index === pathSegments.length - 1,
        };
    });

    if (breadcrumbs.length === 0) {
        return null;
    }

    return (
        <nav aria-label="Breadcrumb" className="hidden md:flex items-center text-sm text-muted-foreground">
            <Link
                href="/dashboard"
                className="flex items-center hover:text-foreground transition-colors"
            >
                <Home className="h-4 w-4" />
                <span className="sr-only">Dashboard</span>
            </Link>

            {breadcrumbs.length > 0 && (
                <ChevronRight className="h-4 w-4 mx-2 text-muted-foreground/50" />
            )}

            {breadcrumbs.map((crumb, index) => (
                <Fragment key={crumb.href}>
                    {crumb.isLast ? (
                        <span className="font-medium text-foreground">{crumb.label}</span>
                    ) : (
                        <Link
                            href={crumb.href}
                            className="hover:text-foreground transition-colors"
                        >
                            {crumb.label}
                        </Link>
                    )}

                    {!crumb.isLast && (
                        <ChevronRight className="h-4 w-4 mx-2 text-muted-foreground/50" />
                    )}
                </Fragment>
            ))}
        </nav>
    );
}
