
'use client';

import { ArrowLeft, Plus, Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useRouter } from 'next/navigation';
import { InventoryCard } from '@/components/inventory/inventory-card';
import { Material } from '@/lib/data';
import { useMemo, useState } from 'react';
import { Skeleton } from '@/components/ui/skeleton';
import { Card } from '@/components/ui/card';
import Link from 'next/link';
import { useTranslation } from '@/lib/i18n/language-context';


export default function InventoryPage() {
  const router = useRouter();
  const { t } = useTranslation();

  const [searchQuery, setSearchQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('all');

  const materials: Material[] = [];
  const isLoading = false;

  const filteredMaterials = useMemo(() => {
    if (!materials) return [];
    return materials
      .filter(material =>
        material.name.toLowerCase().includes(searchQuery.toLowerCase())
      )
      .filter(material =>
        categoryFilter === 'all' ? true : material.category.toLowerCase() === categoryFilter
      );
  }, [materials, searchQuery, categoryFilter]);

  const categories = useMemo(() => {
    if (!materials) return [];
    const uniqueCategories = new Set(materials.map(m => m.category));
    return Array.from(uniqueCategories);
  }, [materials]);


  return (
    <div className="flex flex-col h-full bg-secondary">
      <header className="flex items-center justify-between gap-4 p-4 border-b md:px-6 shrink-0 bg-background sticky top-0 z-10">
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="icon" onClick={() => router.back()} className="h-8 w-8">
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline">
            {t('inventory.title')}
          </h1>
        </div>
        <Link href="/inventory/add">
          <Button>
            <Plus className="mr-2 h-4 w-4" />
            {t('inventory.add_item')}
          </Button>
        </Link>
      </header>

      <div className="p-4 md:px-6 bg-background border-b">
        <div className="flex flex-col sm:flex-row items-center gap-4">
          <div className="relative w-full sm:flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
            <Input
              type="search"
              placeholder={t('inventory.search_placeholder')}
              className="pl-10"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <div className="flex gap-4 w-full sm:w-auto">
            <Select onValueChange={setCategoryFilter} value={categoryFilter}>
              <SelectTrigger className="w-full sm:w-48">
                <SelectValue placeholder={t('inventory.all_categories')} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">{t('inventory.all_categories')}</SelectItem>
                {categories.map(category => (
                  <SelectItem key={category} value={category.toLowerCase()}>{category}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
      </div>

      <main className="flex-1 p-4 overflow-y-auto md:p-6">
        {isLoading && (
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
            {[...Array(6)].map((_, i) => <Skeleton key={i} className="h-48" />)}
          </div>
        )}
        {!isLoading && materials && materials.length > 0 && (
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
            {filteredMaterials.map((material) => (
              <InventoryCard key={material.id} material={material} />
            ))}
          </div>
        )}
        {!isLoading && materials && materials.length === 0 && (
          <Card className="flex flex-col items-center justify-center h-64 text-center p-6 bg-card">
            <h3 className="text-xl font-bold font-headline">{t('inventory.no_items_found')}</h3>
            <p className="max-w-sm mt-2 text-muted-foreground">
              {t('inventory.no_items_desc')}
            </p>
          </Card>
        )}
      </main>
    </div>
  );
}
