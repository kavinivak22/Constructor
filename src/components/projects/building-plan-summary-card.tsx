'use client';

import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { Layers, ArrowRight, CheckSquare, Sparkles, Loader2, Settings2 } from 'lucide-react';
import Link from 'next/link';
import { useSupabase } from '@/supabase/provider';
import { type SubheadingProcess } from '@/app/actions/ai-progress';

interface BuildingPlanSummaryCardProps {
  projectId: string;
  projectName: string;
}

export function BuildingPlanSummaryCard({ projectId, projectName }: BuildingPlanSummaryCardProps) {
  const { supabase } = useSupabase();
  const [processes, setProcesses] = useState<SubheadingProcess[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchScope = async () => {
      if (!projectId) return;
      setIsLoading(true);
      try {
        const { data, error } = await supabase
          .from('projects')
          .select('scope_data')
          .eq('id', projectId)
          .single();

        if (!error && data?.scope_data) {
          const scope = typeof data.scope_data === 'string' ? JSON.parse(data.scope_data) : data.scope_data;
          setProcesses(scope.processes || []);
        }
      } catch (e) {
        console.warn('Failed to fetch building plan summary');
      } finally {
        setIsLoading(false);
      }
    };

    fetchScope();
  }, [projectId, supabase]);

  if (isLoading) {
    return (
      <Card className="glass-card p-4 flex items-center justify-center text-muted-foreground text-xs">
        <Loader2 className="h-4 w-4 animate-spin mr-2" /> Loading building plan summary...
      </Card>
    );
  }

  // Calculate stats
  const totalStages = processes.length;
  let activeStageTitle = 'No Plan Selected';
  let activeStageIndex = 0;
  let totalQualityChecks = 0;
  let verifiedQualityChecks = 0;

  processes.forEach((proc, idx) => {
    const hasUncompleted = proc.tasks.some(t => t.status !== 'completed');
    if (hasUncompleted && activeStageTitle === 'No Plan Selected') {
      activeStageTitle = proc.title;
      activeStageIndex = idx + 1;
    }

    proc.tasks.forEach(t => {
      (t.checklists || []).forEach(chk => {
        totalQualityChecks++;
        if (chk.is_completed) verifiedQualityChecks++;
      });
    });
  });

  const overallProgress = totalQualityChecks > 0 ? Math.round((verifiedQualityChecks / totalQualityChecks) * 100) : 0;

  return (
    <Card className="glass-card border-white/10 rounded-2xl hover:border-white/20 transition-all">
      <CardContent className="p-4 sm:p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-start gap-3 flex-1 min-w-0">
          <div className="w-10 h-10 rounded-xl bg-primary/10 border border-primary/20 flex items-center justify-center text-primary shrink-0 mt-0.5">
            <Layers className="h-5 w-5" />
          </div>
          <div className="space-y-1 min-w-0 flex-1">
            <div className="flex items-center gap-2 flex-wrap">
              <h3 className="font-bold text-sm sm:text-base text-foreground font-headline truncate">
                Construction Stages & Quality Checks
              </h3>
              {totalStages > 0 && (
                <Badge variant="outline" className="text-[10px] bg-primary/10 text-primary border-primary/20 font-semibold px-2 py-0">
                  Stage {activeStageIndex || 1} of {totalStages}
                </Badge>
              )}
            </div>

            {totalStages === 0 ? (
              <p className="text-xs text-muted-foreground">No building plan template selected for this project site.</p>
            ) : (
              <div className="space-y-1.5 pt-0.5 max-w-md">
                <div className="flex items-center justify-between text-xs text-muted-foreground">
                  <span className="truncate">Active: <strong className="text-foreground font-semibold">{activeStageTitle}</strong></span>
                  <span className="font-bold text-primary shrink-0 ml-2">{verifiedQualityChecks}/{totalQualityChecks} Quality Checks</span>
                </div>
                <Progress value={overallProgress} className="h-2 bg-white/10" />
              </div>
            )}
          </div>
        </div>

        <Link href={`/projects/${projectId}/building-plan`} className="w-full sm:w-auto shrink-0">
          <Button size="sm" className="w-full sm:w-auto text-xs font-semibold h-9 px-4 rounded-xl bg-primary hover:bg-primary/90 text-primary-foreground shadow-md gap-1.5">
            <span>View Building Plan</span>
            <ArrowRight className="h-3.5 w-3.5" />
          </Button>
        </Link>
      </CardContent>
    </Card>
  );
}
