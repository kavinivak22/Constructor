
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
    high: 'bg-green-100 text-green-700 border-green-200',
    medium: 'bg-yellow-100 text-yellow-800 border-yellow-200',
    low: 'bg-red-100 text-red-700 border-red-200',
  };
  
  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
    }).format(amount);
  }

  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="bg-orange-100 p-2 rounded-lg">
              <Package className="h-5 w-5 text-orange-600" />
            </div>
            <h3 className="font-bold text-md">{material.name}</h3>
          </div>
           <Badge
            className={cn(
              'capitalize text-xs font-semibold',
              levelVariant[level]
            )}
          >
            {level} Stock
          </Badge>
        </div>
        <div className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
          <div className="text-muted-foreground">Category:</div>
          <div className="font-medium text-right">{material.category}</div>

          <div className="text-muted-foreground">Current Stock:</div>
          <div className="font-medium text-right">
            {material.currentStock} {material.unit}
          </div>

          <div className="text-muted-foreground">Min. Stock:</div>
          <div className="font-medium text-right">
            {material.minStock} {material.unit}
          </div>

          <div className="text-muted-foreground">Unit Cost:</div>
          <div className="font-medium text-right">{formatCurrency(material.costPerUnit)}</div>
          
          <div className="text-muted-foreground">Supplier:</div>
          <div className="font-medium text-right">{material.supplier}</div>
        </div>
      </CardContent>
    </Card>
  );
}
