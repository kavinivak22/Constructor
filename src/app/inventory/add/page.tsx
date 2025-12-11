
'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { Button } from '@/components/ui/button';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { ArrowLeft, Loader2, PlusCircle } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useToast } from '@/hooks/use-toast';
import { useState } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogClose } from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { useTranslation } from '@/lib/i18n/language-context';

export default function AddInventoryItemPage() {
  const { t } = useTranslation();
  const router = useRouter();
  const { toast } = useToast();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [suppliers, setSuppliers] = useState(['Supplier A', 'Supplier B', 'Supplier C']);
  const [newSupplier, setNewSupplier] = useState('');
  const [isNewSupplierDialogOpen, setIsNewSupplierDialogOpen] = useState(false);

  // Schema inside component to use t (not strict requirement if we use hardcoded errors or pass t into schema builder, but this is simpler)
  // For validation messages, we might want to translate them too, but for now focusing on UI strings.
  // We can keep schema static if we don't translate validation errors yet. Let's keep it static for simplicity unless needed.

  const formSchema = z.object({
    name: z.string().min(2, 'Item name must be at least 2 characters.'),
    category: z.string().nonempty('Please select a category.'),
    supplier: z.string().nonempty('Please select a supplier.'),
    unit: z.string().nonempty('Please select a unit.'),
    currentStock: z.preprocess(
      (a) => parseFloat(z.string().parse(a)),
      z.number().min(0, 'Stock cannot be negative.')
    ),
    minStock: z.preprocess(
      (a) => parseFloat(z.string().parse(a)),
      z.number().min(0, 'Stock cannot be negative.')
    ),
    costPerUnit: z.preprocess(
      (a) => parseFloat(z.string().parse(a)),
      z.number().positive('Cost must be a positive number.')
    ),
  });

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: '',
      category: '',
      supplier: '',
      unit: '',
      currentStock: 0,
      minStock: 0,
      costPerUnit: 0,
    },
  });

  const handleAddSupplier = () => {
    if (newSupplier.trim()) {
      const updatedSuppliers = [...suppliers, newSupplier.trim()];
      setSuppliers(updatedSuppliers);
      form.setValue('supplier', newSupplier.trim(), { shouldValidate: true });
      setNewSupplier('');
      setIsNewSupplierDialogOpen(false);
    }
  };

  function onSubmit(values: z.infer<typeof formSchema>) {
    setIsSubmitting(true);
    console.log('Form submitted with values:', values);
    setTimeout(() => {
      toast({
        title: 'Item Added',
        description: `${values.name} has been added to your inventory.`,
      });
      router.push('/inventory');
      setIsSubmitting(false);
    }, 1000);
  }

  return (
    <div className="flex flex-col h-full bg-secondary">
      <header className="flex items-center gap-4 p-4 border-b md:px-6 shrink-0 bg-background sticky top-0 z-10">
        <Button variant="ghost" size="icon" onClick={() => router.back()}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline">
          {t('inventory_add.title')}
        </h1>
      </header>
      <main className="flex-1 p-4 overflow-y-auto md:p-6">
        <div className="max-w-2xl mx-auto">
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
              <Card>
                <CardContent className="p-6 space-y-4">
                  <FormField
                    control={form.control}
                    name="name"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t('inventory_add.name_label')}</FormLabel>
                        <FormControl>
                          <Input placeholder={t('inventory_add.name_placeholder')} {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="category"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t('inventory_add.category_label')}</FormLabel>
                        <Select onValueChange={field.onChange} defaultValue={field.value}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder={t('inventory_add.category_placeholder')} />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="cement">Cement</SelectItem>
                            <SelectItem value="steel">Steel</SelectItem>
                            <SelectItem value="bricks">Bricks</SelectItem>
                            <SelectItem value="sand">Sand</SelectItem>
                            <SelectItem value="aggregates">Aggregates</SelectItem>
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="supplier"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t('inventory_add.supplier_label')}</FormLabel>
                        <Select
                          onValueChange={(value) => {
                            if (value === 'create-new') {
                              setIsNewSupplierDialogOpen(true);
                            } else {
                              field.onChange(value);
                            }
                          }}
                          value={field.value}
                        >
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder={t('inventory_add.supplier_placeholder')} />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            {suppliers.map(supplier => (
                              <SelectItem key={supplier} value={supplier}>{supplier}</SelectItem>
                            ))}
                            <Separator className="my-1" />
                            <SelectItem value="create-new">
                              <div className="flex items-center gap-2">
                                <PlusCircle className="h-4 w-4" />
                                <span>{t('inventory_add.create_new_supplier')}</span>
                              </div>
                            </SelectItem>
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="unit"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t('inventory_add.unit_label')}</FormLabel>
                        <Select onValueChange={field.onChange} defaultValue={field.value}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder={t('inventory_add.unit_placeholder')} />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="kg">Kilogram (kg)</SelectItem>
                            <SelectItem value="ton">Ton</SelectItem>
                            <SelectItem value="bag">Bag</SelectItem>
                            <SelectItem value="piece">Piece</SelectItem>
                            <SelectItem value="cubic-meter">Cubic Meter (m³)</SelectItem>
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
                    <FormField
                      control={form.control}
                      name="currentStock"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>{t('inventory_add.current_stock_label')}</FormLabel>
                          <FormControl>
                            <Input type="number" placeholder="0" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name="minStock"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>{t('inventory_add.min_stock_label')}</FormLabel>
                          <FormControl>
                            <Input type="number" placeholder="0" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                  <FormField
                    control={form.control}
                    name="costPerUnit"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t('inventory_add.cost_label')}</FormLabel>
                        <FormControl>
                          <div className="relative">
                            <span className="absolute inset-y-0 left-0 flex items-center pl-3 text-muted-foreground">₹</span>
                            <Input type="number" placeholder="0.00" className="pl-8" {...field} />
                          </div>
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </CardContent>
              </Card>

              <div className="flex justify-end gap-2">
                <Button variant="outline" type="button" onClick={() => router.back()}>{t('common.cancel')}</Button>
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
                  {t('inventory_add.submit_button')}
                </Button>
              </div>
            </form>
          </Form>

          <Dialog open={isNewSupplierDialogOpen} onOpenChange={setIsNewSupplierDialogOpen}>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>{t('inventory_add.dialog_title')}</DialogTitle>
                <DialogDescription>
                  {t('inventory_add.dialog_desc')}
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-4 py-4">
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="new-supplier-name" className="text-right">
                    {t('inventory_add.dialog_name_label')}
                  </Label>
                  <Input
                    id="new-supplier-name"
                    value={newSupplier}
                    onChange={(e) => setNewSupplier(e.target.value)}
                    className="col-span-3"
                    placeholder="e.g., Acme Building Supplies"
                  />
                </div>
              </div>
              <DialogFooter>
                <DialogClose asChild>
                  <Button type="button" variant="outline">{t('common.cancel')}</Button>
                </DialogClose>
                <Button type="button" onClick={handleAddSupplier}>{t('inventory_add.dialog_submit')}</Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>
      </main>
    </div>
  );
}
