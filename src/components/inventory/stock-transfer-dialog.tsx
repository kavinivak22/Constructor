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
import { useSupabase } from '@/supabase/provider';

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
  const { supabase } = useSupabase();
  const { data: projects = [] } = useProjects();

  const [direction, setDirection] = useState<'wh_to_proj' | 'proj_to_wh' | 'proj_to_proj'>('wh_to_proj');
  const [sourceProjectId, setSourceProjectId] = useState<string>(defaultProjectId || '');
  const [destProjectId, setDestProjectId] = useState<string>('');

  const [category, setCategory] = useState<'material' | 'tool' | 'machinery'>('material');
  const [availableItems, setAvailableItems] = useState<{ id: string; name: string; current_stock: number; unit_of_measurement: string }[]>([]);
  const [selectedItemId, setSelectedItemId] = useState<string>('');
  const [itemName, setItemName] = useState('');
  const [quantity, setQuantity] = useState('');
  const [unit, setUnit] = useState('Bags');
  const [notes, setNotes] = useState('');
  const [isLoadingItems, setIsLoadingItems] = useState(false);
  const [isCustomName, setIsCustomName] = useState(false);

  // Fetch available items from source inventory whenever source or direction changes
  useEffect(() => {
    const fetchSourceInventory = async () => {
      setIsLoadingItems(true);
      try {
        let query = supabase.from('materials').select('id, name, current_stock, unit_of_measurement, category');

        if (direction === 'wh_to_proj') {
          query = query.is('site_id', null);
        } else if ((direction === 'proj_to_wh' || direction === 'proj_to_proj') && sourceProjectId) {
          const { data: proj } = await supabase.from('projects').select('site_id').eq('id', sourceProjectId).single();
          if (proj?.site_id) {
            query = query.eq('site_id', proj.site_id);
          } else {
            setAvailableItems([]);
            setIsLoadingItems(false);
            return;
          }
        } else {
          setAvailableItems([]);
          setIsLoadingItems(false);
          return;
        }

        const { data, error } = await query.order('name', { ascending: true });
        if (!error && data) {
          setAvailableItems(data as any[]);
        }
      } catch (err) {
        console.error('Error fetching source inventory:', err);
      } finally {
        setIsLoadingItems(false);
      }
    };

    if (open) {
      fetchSourceInventory();
    }
  }, [open, direction, sourceProjectId, supabase]);

  // Handle item selection from dropdown
  const handleItemSelect = (itemId: string) => {
    setSelectedItemId(itemId);
    if (itemId === 'custom') {
      setIsCustomName(true);
      setItemName('');
    } else {
      setIsCustomName(false);
      const selected = availableItems.find(i => i.id === itemId);
      if (selected) {
        setItemName(selected.name);
        setUnit(selected.unit_of_measurement || 'Units');
      }
    }
  };

  // Filter items matching the chosen asset category tab
  const categoryFilteredItems = availableItems.filter(item => {
    const itemCat = (item.category || '').toLowerCase();
    if (category === 'material') {
      return itemCat !== 'tool' && itemCat !== 'machinery';
    }
    return itemCat === category;
  });

  // Handle submit
  const handleTransfer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!itemName.trim() || !quantity || Number(quantity) <= 0) {
      toast({
        title: 'Invalid Input',
        description: 'Please select a valid item and quantity.',
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
        description: `Voucher ${res.transferNumber}: Shifted ${quantity} ${unit} of ${itemName}.`,
      });
      setOpen(false);
      setItemName('');
      setQuantity('');
      setNotes('');
      setSelectedItemId('');
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
          <Button className="bg-primary hover:bg-primary/90 text-primary-foreground font-semibold rounded-xl gap-2 w-full sm:w-auto h-9 text-xs">
            <Truck className="h-4 w-4" />
            <span>Transfer Stock / Asset</span>
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-[92vw] max-w-[500px] glass-card border-white/10 max-h-[85vh] overflow-y-auto p-4 sm:p-6 rounded-2xl flex flex-col z-50">
        <DialogHeader className="pb-1 shrink-0">
          <DialogTitle className="text-base sm:text-lg font-bold font-headline flex items-center gap-2">
            <Truck className="h-5 w-5 text-primary" /> Stock & Equipment Transfer
          </DialogTitle>
          <DialogDescription className="text-xs text-muted-foreground">
            Move materials, tools, or machinery between Central Warehouse and project sites.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleTransfer} className="space-y-3.5 py-1 flex-1">
          {/* Direction Selector */}
          <div className="space-y-1">
            <Label className="text-xs font-semibold">Transfer Route</Label>
            <Select value={direction} onValueChange={(v: any) => setDirection(v)}>
              <SelectTrigger className="w-full h-9 text-xs">
                <SelectValue placeholder="Select transfer route" />
              </SelectTrigger>
              <SelectContent className="w-full min-w-[var(--radix-select-trigger-width)]">
                <SelectItem value="wh_to_proj">🏢 Central Warehouse ➔ 🏗️ Project Site</SelectItem>
                <SelectItem value="proj_to_wh">🏗️ Project Site ➔ 🏢 Central Warehouse</SelectItem>
                <SelectItem value="proj_to_proj">🏗️ Project Site A ➔ 🏗️ Project Site B</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* Project Source / Destination Dropdowns */}
          {(direction === 'proj_to_wh' || direction === 'proj_to_proj') && (
            <div className="space-y-1">
              <Label className="text-xs font-semibold">Source Project Site</Label>
              <Select value={sourceProjectId} onValueChange={setSourceProjectId}>
                <SelectTrigger className="w-full h-9 text-xs">
                  <SelectValue placeholder="Select source project" />
                </SelectTrigger>
                <SelectContent className="w-full min-w-[var(--radix-select-trigger-width)]">
                  {projects.map((p) => (
                    <SelectItem key={p.id} value={p.id}>{p.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          {(direction === 'wh_to_proj' || direction === 'proj_to_proj') && (
            <div className="space-y-1">
              <Label className="text-xs font-semibold">Destination Project Site</Label>
              <Select value={destProjectId} onValueChange={setDestProjectId}>
                <SelectTrigger className="w-full h-9 text-xs">
                  <SelectValue placeholder="Select destination project" />
                </SelectTrigger>
                <SelectContent className="w-full min-w-[var(--radix-select-trigger-width)]">
                  {projects.filter(p => p.id !== sourceProjectId).map((p) => (
                    <SelectItem key={p.id} value={p.id}>{p.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          {/* Asset Category Selector */}
          <div className="space-y-1">
            <Label className="text-xs font-semibold">Asset Type</Label>
            <div className="grid grid-cols-3 gap-1.5">
              <Button
                type="button"
                variant={category === 'material' ? 'default' : 'outline'}
                size="sm"
                className="text-xs gap-1 h-8 px-2"
                onClick={() => { setCategory('material'); setUnit('Bags'); }}
              >
                <Package className="h-3.5 w-3.5" /> Material
              </Button>
              <Button
                type="button"
                variant={category === 'tool' ? 'default' : 'outline'}
                size="sm"
                className="text-xs gap-1 h-8 px-2"
                onClick={() => { setCategory('tool'); setUnit('Nos'); }}
              >
                <Wrench className="h-3.5 w-3.5" /> Tool
              </Button>
              <Button
                type="button"
                variant={category === 'machinery' ? 'default' : 'outline'}
                size="sm"
                className="text-xs gap-1 h-8 px-2"
                onClick={() => { setCategory('machinery'); setUnit('Units'); }}
              >
                <Tractor className="h-3.5 w-3.5" /> Machinery
              </Button>
            </div>
          </div>

          {/* Item Dropdown Selector */}
          <div className="space-y-1">
            <Label className="text-xs font-semibold">Select {category === 'material' ? 'Material' : category === 'tool' ? 'Tool' : 'Machinery'} to Transfer</Label>
            <Select value={selectedItemId} onValueChange={handleItemSelect}>
              <SelectTrigger className="w-full h-9 text-xs">
                <SelectValue placeholder={isLoadingItems ? "Loading source inventory..." : "Choose item from source stock"} />
              </SelectTrigger>
              <SelectContent className="w-full min-w-[var(--radix-select-trigger-width)]">
                {categoryFilteredItems.map((item) => (
                  <SelectItem key={item.id} value={item.id}>
                    {item.name} (Stock: {item.current_stock} {item.unit_of_measurement || 'Units'})
                  </SelectItem>
                ))}
                <SelectItem value="custom">✍️ + Enter custom / unlisted asset name</SelectItem>
              </SelectContent>
            </Select>

            {isCustomName && (
              <Input
                placeholder="Enter custom item name..."
                value={itemName}
                onChange={(e) => setItemName(e.target.value)}
                className="h-9 text-xs mt-1.5"
                required
              />
            )}
          </div>

          <div className="grid grid-cols-2 gap-2.5">
            <div className="space-y-1">
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
            <div className="space-y-1">
              <Label className="text-xs font-semibold">Unit of Measure</Label>
              <Input
                placeholder="e.g. Bags, CFT, Nos"
                value={unit}
                onChange={(e) => setUnit(e.target.value)}
                className="h-9 text-xs"
              />
            </div>
          </div>

          <div className="space-y-1">
            <Label className="text-xs font-semibold">Transfer Notes / Reason (Optional)</Label>
            <Textarea
              placeholder="e.g. Shifted for Column Casting at Block B"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="text-xs min-h-[55px]"
            />
          </div>

          <DialogFooter className="pt-2 flex-col sm:flex-row gap-2">
            <Button type="button" variant="outline" size="sm" onClick={() => setOpen(false)} className="w-full sm:w-auto h-8 text-xs">
              Cancel
            </Button>
            <Button type="submit" size="sm" disabled={isSubmitting} className="w-full sm:w-auto h-8 text-xs gap-1.5">
              {isSubmitting ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Truck className="h-3.5 w-3.5" />}
              <span>Confirm & Dispatch Transfer</span>
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
