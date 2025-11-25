
'use client';

import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Building2, AlertCircle, LogOut } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useState, useEffect } from 'react';
import Image from 'next/image';
import { type Company, type User as AppUser } from '@/lib/data';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { useSupabase } from '@/supabase/provider';

export default function RegisterCompanyPage() {
  const { user, isLoading: isUserLoading, supabase } = useSupabase();
  const router = useRouter();

  // Company fields
  const [companyName, setCompanyName] = useState('');
  const [address, setAddress] = useState('');
  const [phone, setPhone] = useState('');
  const [website, setWebsite] = useState('');
  const [businessType, setBusinessType] = useState('');
  const [companySize, setCompanySize] = useState('');

  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleCompanyRegistration = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) {
      setError("You must be logged in to register a company.");
      return;
    }

    setIsLoading(true);
    setError(null);

    const requiredFields = [companyName, address, phone, businessType, companySize];
    if (requiredFields.some(field => !field)) {
      setError("Please fill out all required fields.");
      setIsLoading(false);
      return;
    }

    try {
      // Create the company
      const { data: companyData, error: companyError } = await supabase
        .from('companies')
        .insert({
          name: companyName,
          address,
          phone,
          website,
          businessType: businessType as any,
          companySize: companySize as any,
          ownerId: user.id,
        })
        .select('id')
        .single();

      if (companyError) throw companyError;
      if (!companyData) throw new Error('Failed to create company.');

      const companyId = companyData.id;

      // Upsert the user record (create if doesn't exist, update if it does)
      const { error: userError } = await supabase
        .from('users')
        .upsert({
          id: user.id,
          email: user.email,
          displayName: user.user_metadata?.name || user.user_metadata?.full_name || user.email?.split('@')[0],
          phone: user.user_metadata?.phone || null,
          photoURL: user.user_metadata?.avatar_url || user.user_metadata?.picture || null,
          companyId: companyId,
          role: 'admin', // First user of a company is admin
          projectIds: [],
          permissions: {},
          status: 'active',
          updated_at: new Date().toISOString()
        }, {
          onConflict: 'id'
        })

      if (userError) throw userError;

      // Refresh router to update session state before navigating
      router.refresh();
      // Redirect to dashboard after successful registration
      router.push('/');

    } catch (error: any) {
      setError(error.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = async () => {
    await supabase.auth.signOut();
    router.push('/signup');
  };

  if (isUserLoading) {
    return (
      <div className="w-full min-h-screen flex items-center justify-center">
        <Skeleton className="h-96 w-full max-w-lg" />
      </div>
    );
  }

  return (
    <div className="w-full min-h-screen lg:grid lg:grid-cols-2">
      <div className="flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
        <div className="mx-auto w-full max-w-lg space-y-8">
          <div>
            <div className="flex justify-start mb-6 items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-md bg-primary text-primary-foreground">
                <Building2 className="h-6 w-6" />
              </div>
              <span className="text-2xl font-semibold font-headline text-foreground">Constructor</span>
            </div>
            <h2 className="mt-6 text-3xl font-bold tracking-tight text-foreground font-headline">
              Register your Company
            </h2>
            <p className="mt-2 text-muted-foreground">
              You're not part of a company yet. Register yours to continue, or ask your admin for an invitation.
            </p>
          </div>
          <>
            <form onSubmit={handleCompanyRegistration} className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle className='text-xl'>Company Details</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="company-name">Company Name *</Label>
                    <Input id="company-name" placeholder="Your Company Inc." required value={companyName} onChange={(e) => setCompanyName(e.target.value)} />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="address">Company Address *</Label>
                    <Input id="address" placeholder="123 Main St, Anytown, USA" required value={address} onChange={(e) => setAddress(e.target.value)} />
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="phone">Company Phone *</Label>
                      <Input id="phone" type="tel" placeholder="(123) 456-7890" required value={phone} onChange={(e) => setPhone(e.target.value)} />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="website">Company Website</Label>
                      <Input id="website" type="text" placeholder="www.example.com" value={website} onChange={(e) => setWebsite(e.target.value)} />
                    </div>
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="businessType">Type of Business *</Label>
                      <Select onValueChange={setBusinessType} required value={businessType}>
                        <SelectTrigger id="businessType"><SelectValue placeholder="Select type..." /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="general_contractor">General Contractor</SelectItem>
                          <SelectItem value="sub_contractor">Sub-Contractor</SelectItem>
                          <SelectItem value="developer">Developer</SelectItem>
                          <SelectItem value="architectural_firm">Architectural Firm</SelectItem>
                          <SelectItem value="other">Other</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="companySize">Company Size *</Label>
                      <Select onValueChange={setCompanySize} required value={companySize}>
                        <SelectTrigger id="companySize"><SelectValue placeholder="Select size..." /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="1-10">1-10 employees</SelectItem>
                          <SelectItem value="11-50">11-50 employees</SelectItem>
                          <SelectItem value="51-200">51-200 employees</SelectItem>
                          <SelectItem value="201-500">201-500 employees</SelectItem>
                          <SelectItem value="500+">500+ employees</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {error && (
                <div className="flex items-center gap-2 text-sm text-destructive">
                  <AlertCircle className="h-4 w-4" />
                  <span>{error}</span>
                </div>
              )}
              <div>
                <Button type="submit" className="w-full" size="lg" disabled={isLoading}>
                  {isLoading ? 'Creating Company...' : 'Create Company & Continue'}
                </Button>
              </div>
            </form>
            <div className="mt-6 text-center text-sm text-muted-foreground">
              <p>
                Signed up with the wrong email?
              </p>
              <Button
                variant="link"
                className="font-semibold text-primary"
                onClick={handleLogout}
              >
                <LogOut className="mr-2 h-4 w-4" />
                Try a different email
              </Button>
            </div>
          </>
        </div>
      </div>
      <div className="hidden lg:block relative">
        <Image
          src="https://images.unsplash.com/photo-1519791883288-dc8bd696e667?q=80&w=2940&ixlib=rb-4.1.0"
          alt="Architects reviewing blueprints at a construction site"
          width={1920}
          height={1080}
          className="h-full w-full object-cover"
          data-ai-hint="architect construction"
        />
      </div>
    </div>
  );
}
