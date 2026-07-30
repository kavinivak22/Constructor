
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
import { useMemo, useState, useEffect } from 'react';
import { Skeleton } from '@/components/ui/skeleton';
import { Card } from '@/components/ui/card';
import Link from 'next/link';
import { getInventoryMaterials } from '@/app/actions/materials';
import { useSupabase } from '@/supabase/provider';


import { StockTransferDialog } from '@/components/inventory/stock-transfer-dialog';
import { Package, Wrench, Tractor, Truck } from 'lucide-react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';

export default function InventoryPage() {
  const router = useRouter();
  const { supabase, user } = useSupabase();
  const [currentUserProfile, setCurrentUserProfile] = useState<{ id: string; role: string } | null>(null);
  const [activeAssetTab, setActiveAssetTab] = useState<'material' | 'tool' | 'machinery'>('material');

  useEffect(() => {
    const fetchProfile = async () => {
      if (!user) return;
      const { data } = await supabase
        .from('users')
        .select('id, role')
        .eq('id', user.id)
        .single();
      if (data) {
        setCurrentUserProfile(data);
      }
    };
    fetchProfile();
  }, [user, supabase]);

  const isAdminOrManager = currentUserProfile?.role === 'admin' || currentUserProfile?.role === 'manager';

  const [searchQuery, setSearchQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('all');

  const [materials, setMaterials] = useState<Material[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const loadMaterials = async () => {
    setIsLoading(true);
    try {
      const res = await getInventoryMaterials();
      if (res.success && res.data) {
        setMaterials(res.data as any);
      } else {
        console.error(res.error);
      }
    } catch (err) {
      console.error('Failed to load materials:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadMaterials();
  }, []);

  const filteredMaterials = useMemo(() => {
    if (!materials) return [];
    return materials
      .filter(material =>
        material.name.toLowerCase().includes(searchQuery.toLowerCase())
      )
      .filter(material => {
        if (activeAssetTab === 'material') {
          return material.category.toLowerCase() !== 'tool' && material.category.toLowerCase() !== 'machinery';
        }
        return material.category.toLowerCase() === activeAssetTab;
      })
      .filter(material =>
        categoryFilter === 'all' ? true : material.category.toLowerCase() === categoryFilter
      );
  }, [materials, searchQuery, categoryFilter, activeAssetTab]);

  const categories = useMemo(() => {
    if (!materials) return [];
    const uniqueCategories = new Set(materials.map(m => m.category));
    return Array.from(uniqueCategories);
  }, [materials]);


  return (
    <div className="flex flex-col h-full bg-transparent">
      <header className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-4 md:px-6 shrink-0 bg-transparent sticky top-0 z-10 glass border-b border-white/10">
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="icon" onClick={() => router.back()} className="h-8 w-8">
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <div>
            <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline">
              Central Warehouse
            </h1>
            <p className="text-xs text-muted-foreground">Central company yard, tool shed & machinery fleet stock.</p>
          </div>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          <StockTransferDialog onSuccess={() => loadMaterials()} />

          {isAdminOrManager && (
            <Link href="/inventory/add">
              <Button size="sm" variant="outline" className="gap-1.5">
                <Plus className="h-4 w-4" />
                Add Asset
              </Button>
            </Link>
          )}
        </div>
      </header>
      
      <div className="p-4 md:px-6 bg-transparent space-y-3">
        {/* 3 Asset Classification Tabs */}
        <div className="flex items-center gap-2 border-b border-border/40 pb-2">
          <Button
            variant={activeAssetTab === 'material' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => setActiveAssetTab('material')}
            className="text-xs font-semibold gap-1.5 rounded-xl"
          >
            <Package className="h-4 w-4" /> Materials Stock
          </Button>
          <Button
            variant={activeAssetTab === 'tool' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => setActiveAssetTab('tool')}
            className="text-xs font-semibold gap-1.5 rounded-xl"
          >
            <Wrench className="h-4 w-4" /> Tools & Equipment
          </Button>
          <Button
            variant={activeAssetTab === 'machinery' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => setActiveAssetTab('machinery')}
            className="text-xs font-semibold gap-1.5 rounded-xl"
          >
            <Tractor className="h-4 w-4" /> Heavy Machinery
          </Button>
        </div>

        <div className="flex flex-col sm:flex-row items-center gap-4">
          <div className="relative w-full sm:flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              type="search"
              placeholder={`Search ${activeAssetTab}s...`}
              className="pl-9 h-9 text-xs"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
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
             <Card className="glass-card flex flex-col items-center justify-center h-64 text-center p-6">
                <h3 className="text-xl font-bold font-headline">No Items Found</h3>
                <p className="max-w-sm mt-2 text-muted-foreground">
                    Your inventory is empty. Try adding a new item to get started.
                </p>
            </Card>
        )}
      </main>
    </div>
  );
}
