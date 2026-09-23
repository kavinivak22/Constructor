
'use client';

import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { type Material } from '@/lib/data';
import { cn } from '@/lib/utils';
import { Package } from 'lucide-react';

interface InventoryCardProps {
  material: Material;
}

export function InventoryCard({ material }: InventoryCardProps) {
  const getLevel = () => {
    if (material.currentStock <= material.minStock) return 'low';
    if (material.currentStock <= material.minStock * 1.5) return 'medium';
    return 'high';
  };

  const level = getLevel();

  const levelVariant = {
    high: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20',
    medium: 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20',
    low: 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border-rose-500/20',
  };
  
  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(amount);
  }

  return (
    <Card className="glass-card rounded-2xl border border-white/10 dark:border-white/5 transition-all duration-200 hover:shadow-md">
      <CardContent className="p-3.5 sm:p-5">
        <div className="flex items-start justify-between mb-3 gap-2">
          <div className="flex items-center gap-2.5 min-w-0 flex-1">
            <div className="bg-primary/15 p-2 rounded-xl text-primary shrink-0">
              <Package className="h-4 w-4 sm:h-5 sm:w-5" />
            </div>
            <h3 className="font-bold text-sm sm:text-base truncate text-foreground">{material.name}</h3>
          </div>
           <Badge
            className={cn(
              'capitalize text-[10px] sm:text-xs font-semibold shrink-0 rounded-full px-2 py-0.5 border',
              levelVariant[level]
            )}
          >
            {level} Stock
          </Badge>
        </div>
        <div className="grid grid-cols-2 gap-x-2 gap-y-1.5 text-xs sm:text-sm border-t border-border/40 pt-2.5">
          {(material.siteName || material.projectName) && (
            <>
              <div className="text-muted-foreground">Site:</div>
              <div className="font-medium text-right truncate text-primary">{material.siteName || material.projectName}</div>
            </>
          )}

          <div className="text-muted-foreground">Category:</div>
          <div className="font-medium text-right truncate">{material.category}</div>

          <div className="text-muted-foreground">Current Stock:</div>
          <div className="font-bold text-right text-foreground">
            {material.currentStock} {material.unit}
          </div>

          <div className="text-muted-foreground">Min. Stock:</div>
          <div className="font-medium text-right">
            {material.minStock} {material.unit}
          </div>

          <div className="text-muted-foreground">Unit Cost:</div>
          <div className="font-semibold text-right">{formatCurrency(material.costPerUnit)}</div>
          
          {material.supplier && (
            <>
              <div className="text-muted-foreground">Supplier:</div>
              <div className="font-medium text-right truncate">{material.supplier}</div>
            </>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
