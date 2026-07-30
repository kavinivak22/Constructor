'use client';

import React, { useState } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Sparkles, Calculator, Plus, Zap } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';

interface MixRatioHelperWidgetProps {
  onAddMaterials: (items: { materialName: string; quantityConsumed: number; unit: string }[]) => void;
}

export function MixRatioHelperWidget({ onAddMaterials }: MixRatioHelperWidgetProps) {
  const { toast } = useToast();
  const [activity, setActivity] = useState<'concrete' | 'masonry' | 'plaster'>('concrete');
  const [mixGrade, setMixGrade] = useState<string>('m20');
  
  // Custom ratio parts
  const [customSand, setCustomSand] = useState('2.5');
  const [customGravel, setCustomGravel] = useState('3.5');
  
  const [cementBags, setCementBags] = useState('');

  // Handle Calculate & Add
  const handleCalculateAndAdd = () => {
    const bags = Number(cementBags);
    if (!bags || bags <= 0) {
      toast({
        title: 'Enter Cement Bags',
        description: 'Please enter a valid number of cement bags.',
        variant: 'destructive',
      });
      return;
    }

    let sandRatio = 1.5;
    let gravelRatio = 3.0;
    let sandName = 'M-Sand (Fine Aggregate)';
    let gravelName = '20mm Blue Metal (Coarse Aggregate)';

    if (activity === 'concrete') {
      if (mixGrade === 'm20') { sandRatio = 1.5; gravelRatio = 3.0; }
      else if (mixGrade === 'm25') { sandRatio = 1.0; gravelRatio = 2.0; }
      else if (mixGrade === 'm15') { sandRatio = 2.0; gravelRatio = 4.0; }
      else if (mixGrade === 'custom') {
        sandRatio = Number(customSand) || 2;
        gravelRatio = Number(customGravel) || 4;
      }
    } else if (activity === 'masonry') {
      sandName = 'P-Sand (Masonry Sand)';
      gravelRatio = 0; // No coarse aggregate in mortar
      if (mixGrade === '1:4') sandRatio = 4.0;
      else if (mixGrade === '1:3') sandRatio = 3.0;
      else if (mixGrade === '1:5') sandRatio = 5.0;
      else if (mixGrade === 'custom') sandRatio = Number(customSand) || 4;
    } else if (activity === 'plaster') {
      sandName = 'Plastering Sand (Fine)';
      gravelRatio = 0;
      if (mixGrade === '1:6') sandRatio = 6.0;
      else if (mixGrade === '1:4') sandRatio = 4.0;
      else if (mixGrade === 'custom') sandRatio = Number(customSand) || 6;
    }

    const calculatedSandCFT = Math.round(bags * sandRatio * 1.25 * 10) / 10;
    const calculatedGravelCFT = Math.round(bags * gravelRatio * 1.25 * 10) / 10;

    const itemsToAdd = [
      { materialName: '53 Grade Cement', quantityConsumed: bags, unit: 'Bags' },
      { materialName: sandName, quantityConsumed: calculatedSandCFT, unit: 'CFT' },
    ];

    if (gravelRatio > 0) {
      itemsToAdd.push({ materialName: gravelName, quantityConsumed: calculatedGravelCFT, unit: 'CFT' });
    }

    onAddMaterials(itemsToAdd);

    toast({
      title: 'Aggregates Auto-Calculated!',
      description: `Added ${bags} Cement Bags, ${calculatedSandCFT} CFT Sand${gravelRatio > 0 ? `, and ${calculatedGravelCFT} CFT Blue Metal` : ''} to daily log.`,
    });

    setCementBags('');
  };

  return (
    <Card className="glass-card border-primary/20 bg-primary/5 p-3.5 sm:p-4 rounded-2xl space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-lg bg-primary/20 text-primary flex items-center justify-center">
            <Calculator className="h-4 w-4" />
          </div>
          <h4 className="font-bold text-xs sm:text-sm text-foreground font-headline flex items-center gap-1.5">
            Auto-Calculate Aggregates & Mix Ratio
          </h4>
        </div>
        <Badge variant="outline" className="text-[10px] bg-primary/10 text-primary border-primary/20 font-bold px-2 py-0">
          Smart Auto-Calc
        </Badge>
      </div>

      {/* Recently Used Chips */}
      <div className="flex items-center gap-1.5 flex-wrap pt-0.5">
        <span className="text-[11px] text-muted-foreground font-medium flex items-center gap-1">
          <Zap className="h-3 w-3 text-amber-500" /> Recently Used:
        </span>
        <Badge
          variant="secondary"
          className="text-[10px] cursor-pointer hover:bg-primary/20 transition-colors"
          onClick={() => { setActivity('concrete'); setMixGrade('m20'); }}
        >
          ⚡ M20 (1:1.5:3)
        </Badge>
        <Badge
          variant="secondary"
          className="text-[10px] cursor-pointer hover:bg-primary/20 transition-colors"
          onClick={() => { setActivity('masonry'); setMixGrade('1:4'); }}
        >
          ⚡ 1:4 Masonry Mortar
        </Badge>
        <Badge
          variant="secondary"
          className="text-[10px] cursor-pointer hover:bg-primary/20 transition-colors"
          onClick={() => { setActivity('plaster'); setMixGrade('1:6'); }}
        >
          ⚡ 1:6 Plastering
        </Badge>
      </div>

      {/* Form Fields */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5 pt-1">
        <div className="space-y-1">
          <Label className="text-[11px] font-semibold">Activity Type</Label>
          <Select value={activity} onValueChange={(v: any) => { setActivity(v); setMixGrade(v === 'concrete' ? 'm20' : v === 'masonry' ? '1:4' : '1:6'); }}>
            <SelectTrigger className="h-8 text-xs bg-background/50">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="concrete">🏗️ Concreting (Columns/Slabs)</SelectItem>
              <SelectItem value="masonry">🧱 Brick Masonry Mortar</SelectItem>
              <SelectItem value="plaster">🖌️ Wall Plastering Mortar</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-1">
          <Label className="text-[11px] font-semibold">Mix Ratio / Grade</Label>
          <Select value={mixGrade} onValueChange={setMixGrade}>
            <SelectTrigger className="h-8 text-xs bg-background/50">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {activity === 'concrete' ? (
                <>
                  <SelectItem value="m20">M20 Grade (1 : 1.5 : 3)</SelectItem>
                  <SelectItem value="m25">M25 Grade (1 : 1 : 2)</SelectItem>
                  <SelectItem value="m15">M15 Grade (1 : 2 : 4)</SelectItem>
                  <SelectItem value="custom">⚙️ Custom Mix Ratio</SelectItem>
                </>
              ) : (
                <>
                  <SelectItem value={activity === 'masonry' ? '1:4' : '1:6'}>
                    {activity === 'masonry' ? '1 : 4 Mortar (Standard)' : '1 : 6 Mortar (Standard)'}
                  </SelectItem>
                  <SelectItem value={activity === 'masonry' ? '1:3' : '1:4'}>
                    {activity === 'masonry' ? '1 : 3 Mortar (Rich)' : '1 : 4 Mortar (Rich)'}
                  </SelectItem>
                  <SelectItem value="custom">⚙️ Custom Mix Ratio</SelectItem>
                </>
              )}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-1">
          <Label className="text-[11px] font-semibold">Cement Quantity</Label>
          <div className="flex gap-2">
            <Input
              type="number"
              placeholder="e.g. 10 Bags"
              value={cementBags}
              onChange={(e) => setCementBags(e.target.value)}
              className="h-8 text-xs bg-background/50"
            />
          </div>
        </div>
      </div>

      {/* Custom numerical ratio input if Custom Ratio is selected */}
      {mixGrade === 'custom' && (
        <div className="p-2.5 rounded-xl bg-background/40 border border-white/10 space-y-1.5 animate-in fade-in-50">
          <Label className="text-[11px] font-semibold flex items-center gap-1 text-primary">
            ⚙️ Custom Mix Proportions (Cement : Sand : Gravel)
          </Label>
          <div className="grid grid-cols-3 gap-2 text-xs">
            <div>
              <span className="text-[10px] text-muted-foreground">Cement Part</span>
              <Input disabled value="1" className="h-7 text-xs bg-muted/50" />
            </div>
            <div>
              <span className="text-[10px] text-muted-foreground">Sand Part</span>
              <Input value={customSand} onChange={(e) => setCustomSand(e.target.value)} className="h-7 text-xs" placeholder="e.g. 2.5" />
            </div>
            {activity === 'concrete' && (
              <div>
                <span className="text-[10px] text-muted-foreground">Gravel Part</span>
                <Input value={customGravel} onChange={(e) => setCustomGravel(e.target.value)} className="h-7 text-xs" placeholder="e.g. 3.5" />
              </div>
            )}
          </div>
        </div>
      )}

      <Button
        type="button"
        size="sm"
        onClick={handleCalculateAndAdd}
        className="w-full h-8 text-xs font-semibold bg-primary hover:bg-primary/90 text-primary-foreground gap-1.5 justify-center shadow-md rounded-xl"
      >
        <Plus className="h-3.5 w-3.5" />
        <span>Auto-Calculate & Add Aggregates to Worklog</span>
      </Button>
    </Card>
  );
}
