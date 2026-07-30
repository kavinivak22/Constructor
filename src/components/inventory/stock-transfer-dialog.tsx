'use client';

import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { Truck, ArrowRight, Loader2, Package, Wrench, Tractor } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { executeStockTransfer } from '@/app/actions/transfers';
import { useProjects } from '@/hooks/queries';

interface StockTransferDialogProps {
  trigger?: React.ReactNode;
  defaultSourceType?: 'warehouse' | 'project';
  defaultProjectId?: string;
  onSuccess?: () => void;
}

export function StockTransferDialog({ trigger, defaultSourceType = 'warehouse', defaultProjectId, onSuccess }: StockTransferDialogProps) {
  const [open, setOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { toast } = useToast();
  const { data: projects = [] } = useProjects();

  const [direction, setDirection] = useState<'wh_to_proj' | 'proj_to_wh' | 'proj_to_proj'>('wh_to_proj');
  const [sourceProjectId, setSourceProjectId] = useState<string>(defaultProjectId || '');
  const [destProjectId, setDestProjectId] = useState<string>('');

  const [category, setCategory] = useState<'material' | 'tool' | 'machinery'>('material');
  const [itemName, setItemName] = useState('');
  const [quantity, setQuantity] = useState('');
  const [unit, setUnit] = useState('Bags');
  const [notes, setNotes] = useState('');

  // Handle submit
  const handleTransfer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!itemName.trim() || !quantity || Number(quantity) <= 0) {
      toast({
        title: 'Invalid Input',
        description: 'Please specify item name and a valid quantity.',
        variant: 'destructive',
      });
      return;
    }

    let sourceType: 'warehouse' | 'project' = 'warehouse';
    let sourceId: string | null = null;
    let destinationType: 'warehouse' | 'project' = 'warehouse';
    let destinationId: string | null = null;

    if (direction === 'wh_to_proj') {
      sourceType = 'warehouse';
      destinationType = 'project';
      destinationId = destProjectId;
    } else if (direction === 'proj_to_wh') {
      sourceType = 'project';
      sourceId = sourceProjectId;
      destinationType = 'warehouse';
    } else if (direction === 'proj_to_proj') {
      sourceType = 'project';
      sourceId = sourceProjectId;
      destinationType = 'project';
      destinationId = destProjectId;
    }

    if ((sourceType === 'project' && !sourceId) || (destinationType === 'project' && !destinationId)) {
      toast({
        title: 'Project Required',
        description: 'Please select valid source and destination projects.',
        variant: 'destructive',
      });
      return;
    }

    setIsSubmitting(true);
    const res = await executeStockTransfer({
      sourceType,
      sourceId,
      destinationType,
      destinationId,
      category,
      itemName: itemName.trim(),
      quantity: Number(quantity),
      unit,
      notes: notes.trim(),
    });
    setIsSubmitting(false);

    if (res.success) {
      toast({
        title: 'Transfer Dispatched!',
        description: `Voucher ${res.transferNumber}: Moved ${quantity} ${unit} of ${itemName}.`,
      });
      setOpen(false);
      setItemName('');
      setQuantity('');
      setNotes('');
      if (onSuccess) onSuccess();
    } else {
      toast({
        title: 'Transfer Error',
        description: res.error || 'Failed to complete transfer.',
        variant: 'destructive',
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        {trigger || (
          <Button className="bg-primary hover:bg-primary/90 text-primary-foreground font-semibold rounded-xl gap-2">
            <Truck className="h-4 w-4" />
            <span>Transfer Stock / Asset</span>
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="sm:max-w-[500px] glass-card border-white/10">
        <DialogHeader>
          <DialogTitle className="text-lg font-bold font-headline flex items-center gap-2">
            <Truck className="h-5 w-5 text-primary" /> Stock & Equipment Transfer
          </DialogTitle>
          <DialogDescription className="text-xs text-muted-foreground">
            Move materials, tools, or machinery between Central Warehouse and project sites.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleTransfer} className="space-y-4 py-2">
          {/* Direction Selector */}
          <div className="space-y-1.5">
            <Label className="text-xs font-semibold">Transfer Route</Label>
            <Select value={direction} onValueChange={(v: any) => setDirection(v)}>
              <SelectTrigger className="h-9 text-xs">
                <SelectValue placeholder="Select transfer route" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="wh_to_proj">🏢 Central Warehouse ➔ 🏗️ Project Site</SelectItem>
                <SelectItem value="proj_to_wh">🏗️ Project Site ➔ 🏢 Central Warehouse</SelectItem>
                <SelectItem value="proj_to_proj">🏗️ Project Site A ➔ 🏗️ Project Site B</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* Project Source / Destination Dropdowns */}
          {(direction === 'proj_to_wh' || direction === 'proj_to_proj') && (
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold">Source Project Site</Label>
              <Select value={sourceProjectId} onValueChange={setSourceProjectId}>
                <SelectTrigger className="h-9 text-xs">
                  <SelectValue placeholder="Select source project" />
                </SelectTrigger>
                <SelectContent>
                  {projects.map((p) => (
                    <SelectItem key={p.id} value={p.id}>{p.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          {(direction === 'wh_to_proj' || direction === 'proj_to_proj') && (
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold">Destination Project Site</Label>
              <Select value={destProjectId} onValueChange={setDestProjectId}>
                <SelectTrigger className="h-9 text-xs">
                  <SelectValue placeholder="Select destination project" />
                </SelectTrigger>
                <SelectContent>
                  {projects.filter(p => p.id !== sourceProjectId).map((p) => (
                    <SelectItem key={p.id} value={p.id}>{p.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          {/* Asset Category Selector */}
          <div className="space-y-1.5">
            <Label className="text-xs font-semibold">Asset Type</Label>
            <div className="grid grid-cols-3 gap-2">
              <Button
                type="button"
                variant={category === 'material' ? 'default' : 'outline'}
                size="sm"
                className="text-xs gap-1.5 h-8"
                onClick={() => { setCategory('material'); setUnit('Bags'); }}
              >
                <Package className="h-3.5 w-3.5" /> Material
              </Button>
              <Button
                type="button"
                variant={category === 'tool' ? 'default' : 'outline'}
                size="sm"
                className="text-xs gap-1.5 h-8"
                onClick={() => { setCategory('tool'); setUnit('Nos'); }}
              >
                <Wrench className="h-3.5 w-3.5" /> Tool
              </Button>
              <Button
                type="button"
                variant={category === 'machinery' ? 'default' : 'outline'}
                size="sm"
                className="text-xs gap-1.5 h-8"
                onClick={() => { setCategory('machinery'); setUnit('Units'); }}
              >
                <Tractor className="h-3.5 w-3.5" /> Machinery
              </Button>
            </div>
          </div>

          {/* Item Details */}
          <div className="space-y-1.5">
            <Label className="text-xs font-semibold">Item Name</Label>
            <Input
              placeholder={category === 'material' ? 'e.g. 53 Grade Cement' : category === 'tool' ? 'e.g. Angle Grinder / Scaffolding' : 'e.g. Concrete Mixer 10/7'}
              value={itemName}
              onChange={(e) => setItemName(e.target.value)}
              className="h-9 text-xs"
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold">Quantity</Label>
              <Input
                type="number"
                placeholder="e.g. 20"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                className="h-9 text-xs"
                required
              />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold">Unit of Measure</Label>
              <Input
                placeholder="e.g. Bags, CFT, Nos, Tons"
                value={unit}
                onChange={(e) => setUnit(e.target.value)}
                className="h-9 text-xs"
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label className="text-xs font-semibold">Transfer Notes / Reason (Optional)</Label>
            <Textarea
              placeholder="e.g. Shifted for Column Casting at Block B"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="text-xs min-h-[60px]"
            />
          </div>

          <DialogFooter className="pt-2">
            <Button type="button" variant="outline" size="sm" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" size="sm" disabled={isSubmitting} className="gap-1.5">
              {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Truck className="h-4 w-4" />}
              <span>Confirm & Dispatch Transfer</span>
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
