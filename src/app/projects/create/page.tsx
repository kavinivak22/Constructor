
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
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { Calendar } from '@/components/ui/calendar';
import { ArrowLeft, CalendarIcon, Loader2, Upload } from 'lucide-react';
import { cn } from '@/lib/utils';
import { format } from 'date-fns';
import { useSupabase } from '@/supabase/provider';
import { useRouter } from 'next/navigation';
import { useToast } from '@/hooks/use-toast';
import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { type User as AppUser } from '@/lib/data';
import { compressImage } from '@/lib/compression';
import { useTranslation } from '@/lib/i18n/language-context';

export default function CreateProjectPage() {
  const { t } = useTranslation();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { supabase, user } = useSupabase();
  const router = useRouter();
  const { toast } = useToast();
  const [userProfile, setUserProfile] = useState<AppUser | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [selectedLogo, setSelectedLogo] = useState<File | null>(null);
  const [logoPreviewUrl, setLogoPreviewUrl] = useState<string | null>(null);

  useEffect(() => {
    if (user) {
      const fetchUserProfile = async () => {
        const { data, error } = await supabase
          .from('users')
          .select('*')
          .eq('id', user.id)
          .single();
        if (data) {
          setUserProfile(data as AppUser);
        }
      };
      fetchUserProfile();
    }
  }, [user, supabase]);

  const formSchema = z.object({
    name: z.string().min(2, 'Project name must be at least 2 characters.'),
    projectType: z.string().nonempty('Please select a project type.'),
    description: z.string().min(10, 'Description must be at least 10 characters.'),
    clientName: z.string().optional(),
    clientContact: z.string().optional(),
    location: z.string().optional(),
    startDate: z.date({
      required_error: 'A start date is required.',
    }),
    endDate: z.date({
      required_error: 'An end date is required.',
    }),
    budget: z.preprocess(
      (a) => parseFloat(z.string().parse(a)),
      z.number().positive('Budget must be a positive number.')
    ),
  });

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: '',
      description: '',
      projectType: '',
      clientName: '',
      clientContact: '',
      location: '',
      budget: 0,
    },
  });

  async function onSubmit(values: z.infer<typeof formSchema>) {
    if (!user || !userProfile?.companyId) {
      toast({
        title: 'Error',
        description: 'You must be logged in and part of a company to create a project.',
        variant: 'destructive',
      });
      return;
    }
    setIsSubmitting(true);
    try {
      let imageId = null;

      if (selectedFile) {
        // Compress image
        let fileToUpload = selectedFile;
        try {
          fileToUpload = await compressImage(selectedFile);
        } catch (e) {
          console.warn("Compression failed", e);
        }

        const fileExt = fileToUpload.name.split('.').pop() || 'jpg';
        const fileName = `${Math.random()}.${fileExt}`;
        const filePath = `${user.id}/${fileName}`;

        const { error: uploadError } = await supabase.storage
          .from('project-images')
          .upload(filePath, fileToUpload);

        if (uploadError) throw uploadError;

        const { data: { publicUrl } } = supabase.storage
          .from('project-images')
          .getPublicUrl(filePath);

        imageId = publicUrl;
      }

      let logoId = null;

      if (selectedLogo) {
        // Compress logo
        let logoToUpload = selectedLogo;
        try {
          logoToUpload = await compressImage(selectedLogo);
        } catch (e) {
          console.warn("Logo compression failed", e);
        }

        const fileExt = logoToUpload.name.split('.').pop() || 'jpg';
        const fileName = `logo-${Math.random()}.${fileExt}`;
        const filePath = `${user.id}/${fileName}`;

        const { error: uploadError } = await supabase.storage
          .from('project-images')
          .upload(filePath, logoToUpload);

        if (uploadError) throw uploadError;

        const { data: { publicUrl } } = supabase.storage
          .from('project-images')
          .getPublicUrl(filePath);

        logoId = publicUrl;
      }

      // ... rest of the logic remains same, just ensuring variables are accessible
      const { data: projectData, error: projectError } = await supabase.from('projects').insert([
        {
          ...values,
          status: 'planning', // Default status
          companyId: userProfile.companyId,
          imageId: imageId,
          companyLogo: logoId,
        },
      ]).select().single();

      if (projectError) throw projectError;

      // Add project to creator's project list
      const currentProjectIds = userProfile.projectIds || [];
      const { error: userUpdateError } = await supabase
        .from('users')
        .update({
          projectIds: [...currentProjectIds, projectData.id]
        })
        .eq('id', user.id);

      if (userUpdateError) {
        console.error("Failed to add project to user", userUpdateError);
      }

      toast({
        title: t('project_create.toast_success_title'),
        description: t('project_create.toast_success_desc'),
      });
      router.push('/');
    } catch (error: any) {
      toast({
        title: t('project_create.toast_error_title'),
        description: error.message,
        variant: 'destructive',
      });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="flex flex-col h-full bg-secondary">
      <header className="flex items-center gap-4 p-4 border-b md:px-6 shrink-0 bg-background sticky top-0 z-10">
        <Button variant="ghost" size="icon" onClick={() => router.back()}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline">
          {t('project_create.title')}
        </h1>
      </header>
      <main className="flex-1 p-4 overflow-y-auto md:p-6">
        <div className="max-w-4xl mx-auto">
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle>{t('project_create.section_info')}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <FormField
                    control={form.control}
                    name="name"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t('project_create.name_label')}</FormLabel>
                        <FormControl>
                          <Input placeholder={t('project_create.name_placeholder')} {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="projectType"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t('project_create.type_label')}</FormLabel>
                        <Select onValueChange={field.onChange} defaultValue={field.value}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder={t('project_create.type_placeholder')} />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="residential">Residential</SelectItem>
                            <SelectItem value="commercial">Commercial</SelectItem>
                            <SelectItem value="industrial">Industrial</SelectItem>
                            <SelectItem value="infrastructure">Infrastructure</SelectItem>
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="description"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t('project_create.desc_label')}</FormLabel>
                        <FormControl>
                          <Textarea
                            placeholder={t('project_create.desc_placeholder')}
                            className="resize-none"
                            {...field}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle>{t('project_create.section_client')}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <FormField
                    control={form.control}
                    name="clientName"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t('project_create.client_name_label')}</FormLabel>
                        <FormControl>
                          <Input placeholder={t('project_create.client_name_placeholder')} {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="clientContact"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t('project_create.client_contact_label')}</FormLabel>
                        <FormControl>
                          <Input placeholder={t('project_create.client_contact_placeholder')} {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="location"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t('project_create.location_label')}</FormLabel>
                        <FormControl>
                          <Input placeholder={t('project_create.location_placeholder')} {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle>{t('project_create.section_logo')}</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="flex items-center justify-center w-full">
                    <label htmlFor="logo-upload" className="flex flex-col items-center justify-center w-full h-32 border-2 border-dashed rounded-lg cursor-pointer bg-card hover:bg-muted relative overflow-hidden">
                      {logoPreviewUrl ? (
                        <img src={logoPreviewUrl} alt="Logo Preview" className="absolute inset-0 w-full h-full object-contain p-2 opacity-50 hover:opacity-40 transition-opacity" />
                      ) : null}
                      <div className="flex flex-col items-center justify-center pt-5 pb-6 z-10">
                        <Upload className="w-8 h-8 mb-4 text-muted-foreground" />
                        <p className="mb-2 text-sm text-muted-foreground"><span className="font-semibold">{t('project_create.upload_logo_text')}</span></p>
                        <p className="text-xs text-muted-foreground">PNG, JPG (MAX. 2MB)</p>
                        {selectedLogo && <p className="mt-2 text-sm font-medium text-primary">{selectedLogo.name}</p>}
                      </div>
                      <input
                        id="logo-upload"
                        type="file"
                        className="hidden"
                        accept="image/*"
                        onChange={(e) => {
                          const file = e.target.files?.[0];
                          if (file) {
                            if (file.size > 2 * 1024 * 1024) {
                              toast({
                                title: 'File too large',
                                description: 'Logo must be less than 2MB.',
                                variant: 'destructive',
                              });
                              return;
                            }
                            setSelectedLogo(file);
                            setLogoPreviewUrl(URL.createObjectURL(file));
                          }
                        }}
                      />
                    </label>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle>{t('project_create.section_timeline')}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
                    <FormField
                      control={form.control}
                      name="startDate"
                      render={({ field }) => (
                        <FormItem className="flex flex-col">
                          <FormLabel>{t('project_create.start_date_label')}</FormLabel>
                          <Popover>
                            <PopoverTrigger asChild>
                              <FormControl>
                                <Button
                                  variant={'outline'}
                                  className={cn(
                                    'pl-3 text-left font-normal',
                                    !field.value && 'text-muted-foreground'
                                  )}
                                >
                                  {field.value ? (
                                    format(field.value, 'PPP')
                                  ) : (
                                    <span>{t('project_create.pick_date')}</span>
                                  )}
                                  <CalendarIcon className="w-4 h-4 ml-auto opacity-50" />
                                </Button>
                              </FormControl>
                            </PopoverTrigger>
                            <PopoverContent className="w-auto p-0" align="start">
                              <Calendar
                                mode="single"
                                selected={field.value}
                                onSelect={field.onChange}
                                initialFocus
                              />
                            </PopoverContent>
                          </Popover>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name="endDate"
                      render={({ field }) => (
                        <FormItem className="flex flex-col">
                          <FormLabel>{t('project_create.end_date_label')}</FormLabel>
                          <Popover>
                            <PopoverTrigger asChild>
                              <FormControl>
                                <Button
                                  variant={'outline'}
                                  className={cn(
                                    'pl-3 text-left font-normal',
                                    !field.value && 'text-muted-foreground'
                                  )}
                                >
                                  {field.value ? (
                                    format(field.value, 'PPP')
                                  ) : (
                                    <span>{t('project_create.pick_date')}</span>
                                  )}
                                  <CalendarIcon className="w-4 h-4 ml-auto opacity-50" />
                                </Button>
                              </FormControl>
                            </PopoverTrigger>
                            <PopoverContent className="w-auto p-0" align="start">
                              <Calendar
                                mode="single"
                                selected={field.value}
                                onSelect={field.onChange}
                                initialFocus
                              />
                            </PopoverContent>
                          </Popover>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                  <FormField
                    control={form.control}
                    name="budget"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t('project_create.budget_label')}</FormLabel>
                        <FormControl>
                          <div className="relative">
                            <span className="absolute inset-y-0 left-0 flex items-center pl-3 text-muted-foreground">₹</span>
                            <Input type="number" placeholder="0" className="pl-8" {...field} />
                          </div>
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle>{t('project_create.section_photos')}</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="flex items-center justify-center w-full">
                    <label htmlFor="dropzone-file" className="flex flex-col items-center justify-center w-full h-48 border-2 border-dashed rounded-lg cursor-pointer bg-card hover:bg-muted relative overflow-hidden">
                      {previewUrl ? (
                        <img src={previewUrl} alt="Preview" className="absolute inset-0 w-full h-full object-cover opacity-50 hover:opacity-40 transition-opacity" />
                      ) : null}
                      <div className="flex flex-col items-center justify-center pt-5 pb-6 z-10">
                        <Upload className="w-8 h-8 mb-4 text-muted-foreground" />
                        <p className="mb-2 text-sm text-muted-foreground"><span className="font-semibold">{t('project_create.upload_photo_text')}</span> or drag and drop</p>
                        <p className="text-xs text-muted-foreground">SVG, PNG, JPG or GIF (MAX. 5MB)</p>
                        {selectedFile && <p className="mt-2 text-sm font-medium text-primary">{selectedFile.name}</p>}
                      </div>
                      <input
                        id="dropzone-file"
                        type="file"
                        className="hidden"
                        accept="image/*"
                        onChange={(e) => {
                          const file = e.target.files?.[0];
                          if (file) {
                            if (file.size > 5 * 1024 * 1024) {
                              toast({
                                title: 'File too large',
                                description: 'Image must be less than 5MB.',
                                variant: 'destructive',
                              });
                              return;
                            }
                            setSelectedFile(file);
                            setPreviewUrl(URL.createObjectURL(file));
                          }
                        }}
                      />
                    </label>
                  </div>
                </CardContent>
              </Card>

              <div className="flex justify-end gap-2">
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
                  {t('project_create.submit_button')}
                </Button>
              </div>
            </form>
          </Form>
        </div>
      </main>
    </div>
  );
}
