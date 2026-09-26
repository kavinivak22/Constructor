'use client';

import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
  CommandSeparator,
} from '@/components/ui/command';
import { Check, ChevronsUpDown, Plus, Store, Phone, User, Building, CreditCard, Hash, Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import { getMaterialSuppliers, createContractor } from '@/app/actions/contractors';
import { useToast } from '@/hooks/use-toast';

export interface SupplierItem {
  id?: string;
  name: string;
  contactPerson?: string;
  phone?: string;
  accountDetails?: string;
  gst?: string;
}

interface SupplierComboboxProps {
  value: string;
  onChange: (supplierName: string, supplierItem?: SupplierItem) => void;
  className?: string;
}

export function SupplierCombobox({ value, onChange, className }: SupplierComboboxProps) {
  const { toast } = useToast();
  const [open, setOpen] = useState(false);
  const [suppliers, setSuppliers] = useState<SupplierItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  // New Supplier Dialog State
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [newSupplier, setNewSupplier] = useState({
    name: '',
    contactPerson: '',
    phone: '',
    accountDetails: '',
    gst: '',
  });

  const fetchSuppliers = async () => {
    setIsLoading(true);
    try {
      const res = await getMaterialSuppliers();
      if (res.success && res.data) {
        setSuppliers(res.data);
      }
    } catch (err) {
      console.error('Error fetching suppliers:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchSuppliers();
  }, []);

  const handleCreateSupplier = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newSupplier.name.trim()) {
      toast({ title: 'Name Required', description: 'Please enter the supplier name.', variant: 'destructive' });
      return;
    }
    if (!newSupplier.phone.trim()) {
      toast({ title: 'Phone Required', description: 'Please enter the supplier contact phone.', variant: 'destructive' });
      return;
    }

    setIsSubmitting(true);
    try {
      const res = await createContractor({
        name: newSupplier.name.trim(),
        contactPerson: newSupplier.contactPerson.trim() || undefined,
        phone: newSupplier.phone.trim(),
        accountDetails: newSupplier.accountDetails.trim() || undefined,
        gst: newSupplier.gst.trim() || undefined,
        category: 'Material Supplier',
      });

      if (res.success) {
        toast({
          title: 'Supplier Added',
          description: `${newSupplier.name} has been added to your suppliers.`,
        });

        const createdItem: SupplierItem = {
          id: res.data?.id,
          name: newSupplier.name.trim(),
          contactPerson: newSupplier.contactPerson.trim(),
          phone: newSupplier.phone.trim(),
          accountDetails: newSupplier.accountDetails.trim(),
          gst: newSupplier.gst.trim(),
        };

        setSuppliers((prev) => [createdItem, ...prev]);
        onChange(createdItem.name, createdItem);

        // Reset and close
        setNewSupplier({
          name: '',
          contactPerson: '',
          phone: '',
          accountDetails: '',
          gst: '',
        });
        setIsAddDialogOpen(false);
        setOpen(false);
      } else {
        toast({
          title: 'Error',
          description: res.error || 'Failed to create supplier.',
          variant: 'destructive',
        });
      }
    } catch (err: any) {
      toast({
        title: 'Error',
        description: err.message || 'Failed to create supplier.',
        variant: 'destructive',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const selectedSupplier = suppliers.find(
    (s) => s.name.toLowerCase() === (value || '').toLowerCase()
  );

  return (
    <div className="space-y-1">
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <Button
            variant="outline"
            role="combobox"
            aria-expanded={open}
            className={cn(
              'w-full justify-between h-9 text-xs sm:text-sm rounded-xl font-normal bg-background/50',
              !value && 'text-muted-foreground',
              className
            )}
          >
            <span className="truncate flex items-center gap-1.5">
              <Store className="h-3.5 w-3.5 text-primary shrink-0" />
              {value || 'Select supplier...'}
            </span>
            <ChevronsUpDown className="ml-2 h-3.5 w-3.5 shrink-0 opacity-50" />
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-[300px] sm:w-[360px] p-0 glass rounded-2xl shadow-2xl border-white/20 dark:border-white/10" align="start">
          <Command>
            <CommandInput placeholder="Search supplier..." className="h-9 text-xs sm:text-sm" />
            <CommandList className="max-h-[220px]">
              <CommandEmpty className="py-2.5 text-center text-xs text-muted-foreground">
                No supplier found.
              </CommandEmpty>
              <CommandGroup heading="Suppliers">
                {suppliers.map((supplier) => (
                  <CommandItem
                    key={supplier.id || supplier.name}
                    value={supplier.name}
                    onSelect={() => {
                      onChange(supplier.name, supplier);
                      setOpen(false);
                    }}
                    className="flex items-center justify-between text-xs sm:text-sm cursor-pointer py-1.5 px-2 rounded-lg"
                  >
                    <div className="flex flex-col min-w-0 pr-2">
                      <span className="font-semibold truncate">{supplier.name}</span>
                      {(supplier.contactPerson || supplier.phone) && (
                        <span className="text-[11px] text-muted-foreground truncate">
                          {[supplier.contactPerson, supplier.phone].filter(Boolean).join(' • ')}
                        </span>
                      )}
                    </div>
                    <Check
                      className={cn(
                        'h-4 w-4 text-primary shrink-0',
                        (value || '').toLowerCase() === supplier.name.toLowerCase() ? 'opacity-100' : 'opacity-0'
                      )}
                    />
                  </CommandItem>
                ))}
              </CommandGroup>
              <CommandSeparator />
              <div className="p-1">
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="w-full justify-start text-xs font-semibold text-primary hover:text-primary hover:bg-primary/10 rounded-lg h-8"
                  onClick={() => {
                    setOpen(false);
                    setIsAddDialogOpen(true);
                  }}
                >
                  <Plus className="h-3.5 w-3.5 mr-1.5" />
                  + Add New Supplier
                </Button>
              </div>
            </CommandList>
          </Command>
        </PopoverContent>
      </Popover>

      {/* Add New Supplier Modal (Strictly 5 fields) */}
      <Dialog open={isAddDialogOpen} onOpenChange={setIsAddDialogOpen}>
        <DialogContent className="w-[94vw] max-w-[460px] p-5 sm:p-6 rounded-2xl glass border border-white/20 dark:border-white/10 shadow-2xl">
          <form onSubmit={handleCreateSupplier} className="space-y-4">
            <DialogHeader className="space-y-1 pb-1">
              <DialogTitle className="flex items-center gap-2 text-base sm:text-lg font-bold font-headline">
                <Store className="h-4 w-4 text-primary" />
                Add New Supplier
              </DialogTitle>
              <DialogDescription className="text-xs text-muted-foreground">
                Enter details for this supplier. Saved vendors can be reused across all projects.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-3 py-1">
              {/* 1. Name */}
              <div className="space-y-1.5">
                <Label htmlFor="sup-name" className="text-xs font-semibold flex items-center gap-1.5">
                  <Building className="h-3.5 w-3.5 text-primary" />
                  Supplier / Company Name <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="sup-name"
                  placeholder="e.g. UltraTech Traders & Co"
                  value={newSupplier.name}
                  onChange={(e) => setNewSupplier({ ...newSupplier, name: e.target.value })}
                  className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                  required
                />
              </div>

              {/* 2. Contact Person */}
              <div className="space-y-1.5">
                <Label htmlFor="sup-contact" className="text-xs font-semibold flex items-center gap-1.5">
                  <User className="h-3.5 w-3.5 text-primary" />
                  Contact Person
                </Label>
                <Input
                  id="sup-contact"
                  placeholder="e.g. Murugan"
                  value={newSupplier.contactPerson}
                  onChange={(e) => setNewSupplier({ ...newSupplier, contactPerson: e.target.value })}
                  className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                />
              </div>

              {/* 3. Phone Number */}
              <div className="space-y-1.5">
                <Label htmlFor="sup-phone" className="text-xs font-semibold flex items-center gap-1.5">
                  <Phone className="h-3.5 w-3.5 text-primary" />
                  Phone Number <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="sup-phone"
                  placeholder="e.g. +91 98765 43210"
                  value={newSupplier.phone}
                  onChange={(e) => setNewSupplier({ ...newSupplier, phone: e.target.value })}
                  className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                  required
                />
              </div>

              {/* 4. Account Details */}
              <div className="space-y-1.5">
                <Label htmlFor="sup-account" className="text-xs font-semibold flex items-center gap-1.5">
                  <CreditCard className="h-3.5 w-3.5 text-primary" />
                  Account Details
                </Label>
                <Input
                  id="sup-account"
                  placeholder="Bank A/C, IFSC, or UPI ID"
                  value={newSupplier.accountDetails}
                  onChange={(e) => setNewSupplier({ ...newSupplier, accountDetails: e.target.value })}
                  className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                />
              </div>

              {/* 5. GST */}
              <div className="space-y-1.5">
                <Label htmlFor="sup-gst" className="text-xs font-semibold flex items-center gap-1.5">
                  <Hash className="h-3.5 w-3.5 text-primary" />
                  GST Number
                </Label>
                <Input
                  id="sup-gst"
                  placeholder="e.g. 33AAAAA0000A1Z5"
                  value={newSupplier.gst}
                  onChange={(e) => setNewSupplier({ ...newSupplier, gst: e.target.value })}
                  className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                />
              </div>
            </div>

            <DialogFooter className="pt-2 flex flex-col-reverse sm:flex-row gap-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsAddDialogOpen(false)}
                disabled={isSubmitting}
                className="h-9 text-xs sm:text-sm rounded-xl"
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={isSubmitting}
                className="h-9 text-xs sm:text-sm rounded-xl font-semibold"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="mr-1.5 h-3.5 w-3.5 animate-spin" />
                    Saving Supplier...
                  </>
                ) : (
                  'Save & Select Supplier'
                )}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
