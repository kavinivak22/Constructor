'use client';

import React from 'react';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { CheckCircle2, Trash2, AlertCircle, Sparkles, Loader2 } from 'lucide-react';
import type { StagedVoiceAction } from '@/lib/ai-tools/registry';
import { useI18n } from '@/lib/i18n-context';

interface BatchConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  stagedActions: StagedVoiceAction[];
  onRemoveAction: (id: string) => void;
  onConfirmAll: () => Promise<void>;
  isExecuting: boolean;
}

export function BatchConfirmModal({
  isOpen,
  onClose,
  stagedActions,
  onRemoveAction,
  onConfirmAll,
  isExecuting
}: BatchConfirmModalProps) {
  const { language, t } = useI18n();

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && !isExecuting && onClose()}>
      <DialogContent className="max-w-xl glass border-primary/20 bg-background/95 backdrop-blur-xl">
        <DialogHeader>
          <div className="flex items-center gap-2 text-primary mb-1">
            <Sparkles className="h-5 w-5 animate-pulse text-amber-500" />
            <Badge variant="outline" className="border-amber-400/40 bg-amber-500/10 text-amber-600 dark:text-amber-400">
              {t('voiceSessionSummary', 'Voice Session Summary')}
            </Badge>
          </div>
          <DialogTitle className="text-xl font-bold font-headline">
            {language === 'ta' ? 'குரல் வழியே பதிவு செய்யப்பட்ட செயல்களை உறுதிப்படுத்தவும்' : 'Review & Confirm Voice Actions'}
          </DialogTitle>
          <DialogDescription>
            {language === 'ta'
              ? 'குரல் உதவியாளர் வழியே பரிந்துரைக்கப்பட்ட கீழ்க்கண்ட பதிவுகளை சரிபார்த்து உறுதிசெய்யவும்.'
              : 'Review the staged operations extracted during your voice assistant session before committing to the database.'}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-3 max-h-[350px] overflow-y-auto pr-1 my-2">
          {stagedActions.length === 0 ? (
            <div className="flex flex-col items-center justify-center p-6 text-center text-muted-foreground border border-dashed rounded-xl">
              <CheckCircle2 className="h-8 w-8 text-muted-foreground/40 mb-2" />
              <p className="text-sm font-medium">{t('noStagedActions', 'No pending changes staged in this session.')}</p>
            </div>
          ) : (
            stagedActions.map((action, idx) => (
              <Card key={action.id} className="border-primary/15 bg-muted/20 hover:border-primary/30 transition-all">
                <CardContent className="p-3.5 flex items-center justify-between gap-3">
                  <div className="space-y-1 flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <Badge variant="secondary" className="text-[10px] uppercase font-semibold">
                        #{idx + 1} {action.toolName.replace('stage_', '').replace('_', ' ')}
                      </Badge>
                    </div>
                    <p className="text-sm font-semibold text-foreground truncate">
                      {language === 'ta' ? action.summaryTa || action.summary : action.summary}
                    </p>
                    <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
                      {Object.entries(action.params || {}).map(([key, val]) => (
                        <span key={key} className="bg-background/80 px-1.5 py-0.5 rounded border border-muted/30">
                          {key}: <strong className="text-foreground">{String(val)}</strong>
                        </span>
                      ))}
                    </div>
                  </div>
                  <Button
                    size="icon"
                    variant="ghost"
                    className="h-8 w-8 text-muted-foreground hover:text-red-500 hover:bg-red-500/10 shrink-0"
                    onClick={() => onRemoveAction(action.id)}
                    disabled={isExecuting}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </CardContent>
              </Card>
            ))
          )}
        </div>

        <DialogFooter className="flex flex-col sm:flex-row gap-2 pt-2 border-t border-border/40">
          <Button
            variant="outline"
            onClick={onClose}
            disabled={isExecuting}
            className="w-full sm:w-auto"
          >
            {language === 'ta' ? 'ரத்து செய்' : 'Discard / Close'}
          </Button>
          {stagedActions.length > 0 && (
            <Button
              onClick={onConfirmAll}
              disabled={isExecuting}
              className="w-full sm:w-auto bg-primary hover:bg-primary/90 text-primary-foreground gap-2"
            >
              {isExecuting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span>{language === 'ta' ? 'சேமிக்கப்படுகிறது...' : 'Saving Changes...'}</span>
                </>
              ) : (
                <>
                  <CheckCircle2 className="h-4 w-4" />
                  <span>{language === 'ta' ? 'அனைத்தையும் உறுதி செய்' : `Confirm All (${stagedActions.length})`}</span>
                </>
              )}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
