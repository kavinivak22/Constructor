'use client';

import { useState, useEffect } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import {
  Package,
  Tag,
  Layers,
  Truck,
  IndianRupee,
  Loader2,
  Trash2,
  AlertTriangle,
  Edit3,
} from 'lucide-react';
import { SupplierCombobox } from './supplier-combobox';
import { useSupabase } from '@/supabase/provider';
import { useToast } from '@/hooks/use-toast';

export interface EditableMaterial {
  id: string;
  name: string;
  category: string;
  quantity: number;
  min_quantity: number;
  unit: string;
  supplier: string;
  cost: number;
}

interface EditMaterialDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  material: EditableMaterial | null;
  onSuccess: () => void;
}

export function EditMaterialDialog({
  open,
  onOpenChange,
  material,
  onSuccess,
}: EditMaterialDialogProps) {
  const { supabase } = useSupabase();
  const { toast } = useToast();

  const [formData, setFormData] = useState({
    name: '',
    category: '',
    quantity: '',
    min_quantity: '',
    unit: '',
    supplier: '',
    cost: '',
  });

  const [isSaving, setIsSaving] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [showQuantityWarning, setShowQuantityWarning] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  useEffect(() => {
    if (material) {
      setFormData({
        name: material.name || '',
        category: material.category || '',
        quantity: material.quantity?.toString() || '0',
        min_quantity: material.min_quantity?.toString() || '0',
        unit: material.unit || '',
        supplier: material.supplier || '',
        cost: material.cost?.toString() || '0',
      });
    }
  }, [material, open]);

  if (!material) return null;

  const originalQty = Number(material.quantity || 0);
  const newQty = Number(formData.quantity || 0);
  const isQuantityChanged = !isNaN(newQty) && newQty !== originalQty;

  const performSave = async (finalQuantity: number) => {
    setIsSaving(true);
    try {
      const updatePayload: Record<string, any> = {
        name: formData.name.trim(),
        category: formData.category.trim(),
        current_stock: finalQuantity,
        minimum_stock_level: Number(formData.min_quantity) || 0,
        unit_of_measurement: formData.unit.trim(),
        supplier_name: formData.supplier.trim(),
        unit_cost: Number(formData.cost) || 0,
      };

      const { error } = await supabase
        .from('materials')
        .update(updatePayload)
        .eq('id', material.id);

      if (error) throw error;

      toast({
        title: 'Material Updated',
        description: `${formData.name} has been updated successfully.`,
      });

      onOpenChange(false);
      onSuccess();
    } catch (err: any) {
      console.error('Error updating material:', err);
      toast({
        title: 'Update Failed',
        description: err.message || 'Failed to update material.',
        variant: 'destructive',
      });
    } finally {
      setIsSaving(false);
      setShowQuantityWarning(false);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.name.trim()) {
      toast({ title: 'Name Required', description: 'Item name cannot be empty.', variant: 'destructive' });
      return;
    }
    if (!formData.unit.trim()) {
      toast({ title: 'Unit Required', description: 'Please specify a unit of measurement.', variant: 'destructive' });
      return;
    }

    // Check if quantity changed
    if (isQuantityChanged) {
      setShowQuantityWarning(true);
      return;
    }

    // Otherwise, save immediately
    performSave(originalQty);
  };

  const handleDelete = async () => {
    setIsDeleting(true);
    try {
      const { error } = await supabase
        .from('materials')
        .delete()
        .eq('id', material.id);

      if (error) throw error;

      toast({
        title: 'Material Deleted',
        description: `${material.name} was removed from project inventory.`,
      });

      setShowDeleteConfirm(false);
      onOpenChange(false);
      onSuccess();
    } catch (err: any) {
      console.error('Error deleting material:', err);
      toast({
        title: 'Delete Failed',
        description: err.message || 'Failed to delete material.',
        variant: 'destructive',
      });
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="w-[95vw] max-w-[580px] max-h-[90vh] p-4 sm:p-6 rounded-2xl glass border border-white/20 dark:border-white/10 shadow-2xl overflow-y-auto">
          <form onSubmit={handleSubmit}>
            <DialogHeader className="space-y-1 pb-2">
              <div className="flex items-center justify-between">
                <DialogTitle className="flex items-center gap-2 text-base sm:text-xl font-bold font-headline">
                  <Edit3 className="h-4 w-4 sm:h-5 sm:w-5 text-primary" />
                  Edit Material Details
                </DialogTitle>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8 text-destructive hover:bg-destructive/10 rounded-xl"
                  onClick={() => setShowDeleteConfirm(true)}
                  title="Delete material"
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
              <DialogDescription className="text-xs text-muted-foreground">
                Correct item specifications, unit costs, supplier info, or thresholds.
              </DialogDescription>
            </DialogHeader>

            <div className="grid gap-4 py-3 sm:py-4">
              {/* Section 1: Item Details */}
              <div className="space-y-3">
                <div className="flex items-center gap-2 text-xs sm:text-sm font-semibold text-muted-foreground uppercase tracking-wider">
                  <Tag className="h-3.5 w-3.5 text-primary" />
                  Item Details
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
                  <div className="space-y-1.5">
                    <Label htmlFor="edit-name" className="text-xs sm:text-sm font-semibold">
                      Item Name <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="edit-name"
                      value={formData.name}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      placeholder="e.g. Portland Cement"
                      className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                      required
                    />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="edit-category" className="text-xs sm:text-sm font-semibold">
                      Category
                    </Label>
                    <Input
                      id="edit-category"
                      value={formData.category}
                      onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                      placeholder="e.g. Structural"
                      className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                    />
                  </div>
                </div>
              </div>

              <Separator />

              {/* Section 2: Inventory Control */}
              <div className="space-y-3">
                <div className="flex items-center gap-2 text-xs sm:text-sm font-semibold text-muted-foreground uppercase tracking-wider">
                  <Layers className="h-3.5 w-3.5 text-primary" />
                  Inventory Control
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4">
                  <div className="space-y-1.5">
                    <Label htmlFor="edit-quantity" className="text-xs sm:text-sm font-semibold flex items-center justify-between">
                      <span>Stock Qty</span>
                      {isQuantityChanged && (
                        <span className="text-[10px] text-amber-500 font-normal">Modified</span>
                      )}
                    </Label>
                    <Input
                      id="edit-quantity"
                      type="number"
                      value={formData.quantity}
                      onChange={(e) => setFormData({ ...formData, quantity: e.target.value })}
                      placeholder="0"
                      className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                      required
                      step="any"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="edit-min_quantity" className="text-xs sm:text-sm font-semibold">
                      Min Alert Qty <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="edit-min_quantity"
                      type="number"
                      value={formData.min_quantity}
                      onChange={(e) => setFormData({ ...formData, min_quantity: e.target.value })}
                      placeholder="5"
                      className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                      required
                      step="any"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="edit-unit" className="text-xs sm:text-sm font-semibold">
                      Unit <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="edit-unit"
                      value={formData.unit}
                      onChange={(e) => setFormData({ ...formData, unit: e.target.value })}
                      placeholder="e.g. bags"
                      className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                      required
                    />
                  </div>
                </div>
              </div>

              <Separator />

              {/* Section 3: Procurement & Supplier */}
              <div className="space-y-3">
                <div className="flex items-center gap-2 text-xs sm:text-sm font-semibold text-muted-foreground uppercase tracking-wider">
                  <Truck className="h-3.5 w-3.5 text-primary" />
                  Procurement & Supplier
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
                  <div className="space-y-1.5">
                    <Label className="text-xs sm:text-sm font-semibold">Supplier Name</Label>
                    <SupplierCombobox
                      value={formData.supplier}
                      onChange={(name) => setFormData({ ...formData, supplier: name })}
                    />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="edit-cost" className="text-xs sm:text-sm font-semibold">
                      Cost per Unit (₹)
                    </Label>
                    <div className="relative">
                      <IndianRupee className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                      <Input
                        id="edit-cost"
                        type="number"
                        className="pl-8 h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                        value={formData.cost}
                        onChange={(e) => setFormData({ ...formData, cost: e.target.value })}
                        placeholder="0.00"
                        step="any"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <DialogFooter className="pt-2 flex flex-col-reverse sm:flex-row gap-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={isSaving}
                className="h-9 text-xs sm:text-sm rounded-xl"
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={isSaving}
                className="h-9 text-xs sm:text-sm rounded-xl font-semibold"
              >
                {isSaving ? (
                  <>
                    <Loader2 className="mr-1.5 h-3.5 w-3.5 animate-spin" />
                    Saving Changes...
                  </>
                ) : (
                  'Save Changes'
                )}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Confirmation Dialog: Quantity Modification Warning */}
      <AlertDialog open={showQuantityWarning} onOpenChange={setShowQuantityWarning}>
        <AlertDialogContent className="max-w-md rounded-2xl border-white/20 dark:border-white/10 shadow-2xl">
          <AlertDialogHeader>
            <AlertDialogTitle className="flex items-center gap-2 text-base font-bold text-amber-500">
              <AlertTriangle className="h-5 w-5 shrink-0" />
              Stock Quantity Changed
            </AlertDialogTitle>
            <AlertDialogDescription className="text-xs sm:text-sm space-y-2 text-muted-foreground pt-1">
              <p>
                You are modifying the current stock from{' '}
                <strong className="text-foreground">{originalQty} {material.unit}</strong> to{' '}
                <strong className="text-foreground">{newQty} {formData.unit}</strong>.
              </p>
              <p className="bg-amber-500/10 text-amber-600 dark:text-amber-400 p-2.5 rounded-xl border border-amber-500/20 text-xs">
                <strong>Notice:</strong> Directly changing stock here will <u>not</u> log any purchase expense or material usage record in project financials.
              </p>
              <p className="text-xs">
                • To record purchases or restocks with expenses, please use <strong>Add Stock</strong>.<br />
                • To record material consumption, please use <strong>Use Stock</strong>.
              </p>
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter className="flex-col sm:flex-row gap-2 pt-2">
            <AlertDialogCancel
              disabled={isSaving}
              className="mt-0 h-9 rounded-xl text-xs sm:text-sm"
              onClick={() => setShowQuantityWarning(false)}
            >
              Back to Editing
            </AlertDialogCancel>
            <Button
              type="button"
              variant="outline"
              disabled={isSaving}
              className="h-9 rounded-xl text-xs sm:text-sm border-primary/30 text-primary hover:bg-primary/10"
              onClick={() => performSave(originalQty)}
            >
              Revert Quantity & Save Other Edits
            </Button>
            <AlertDialogAction
              disabled={isSaving}
              className="bg-amber-600 text-white hover:bg-amber-700 h-9 rounded-xl text-xs sm:text-sm font-semibold"
              onClick={() => performSave(newQty)}
            >
              Confirm Manual Overwrite
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Confirmation Dialog: Delete Material */}
      <AlertDialog open={showDeleteConfirm} onOpenChange={setShowDeleteConfirm}>
        <AlertDialogContent className="max-w-sm rounded-2xl border-white/20 dark:border-white/10 shadow-2xl">
          <AlertDialogHeader>
            <AlertDialogTitle className="flex items-center gap-2 text-base font-bold text-destructive">
              <Trash2 className="h-4 w-4" />
              Delete Material?
            </AlertDialogTitle>
            <AlertDialogDescription className="text-xs sm:text-sm text-muted-foreground">
              Are you sure you want to delete <strong>{material.name}</strong> from project inventory? This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter className="flex-row gap-2 justify-end pt-2">
            <AlertDialogCancel disabled={isDeleting} className="mt-0 h-9 rounded-xl text-xs sm:text-sm">
              Cancel
            </AlertDialogCancel>
            <AlertDialogAction
              disabled={isDeleting}
              onClick={handleDelete}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90 h-9 rounded-xl text-xs sm:text-sm font-semibold"
            >
              {isDeleting ? (
                <>
                  <Loader2 className="mr-1.5 h-3.5 w-3.5 animate-spin" />
                  Deleting...
                </>
              ) : (
                'Delete'
              )}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
