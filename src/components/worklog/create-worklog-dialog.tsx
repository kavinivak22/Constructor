'use client';

import { useState, useEffect } from 'react';
import { useForm, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { format } from 'date-fns';
import {
    CalendarIcon,
    Plus,
    Trash2,
    Loader2,
    Upload,
    Users,
    Package,
    Image as ImageIcon,
    Calendar as CalendarLucide,
    CheckCircle2,
    ArrowLeft
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Calendar } from '@/components/ui/calendar';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
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
    Popover,
    PopoverContent,
    PopoverTrigger,
} from '@/components/ui/popover';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/use-toast';
import { createWorklog, updateWorklog } from '@/app/actions/worklogs';
import { getProjectMaterials } from '@/app/actions/materials';
import { getContractors } from '@/app/actions/contractors';
import { useSupabase } from '@/supabase/provider';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useProjects } from '@/hooks/queries';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { CreateContractorDialog } from '@/components/contractors/create-contractor-dialog';
import { compressImage } from '@/lib/compression';

// --- Schemas (Matching Server Action) ---

const workerCountSchema = z.object({
    workerType: z.string().min(1, "Worker type is required"),
    count: z.coerce.number().min(0, "Count must be non-negative"),
});

const laborEntrySchema = z.object({
    contractorName: z.string().min(1, "Contractor name is required"),
    category: z.string().optional(),
    workDescription: z.string().optional(),
    paymentStatus: z.enum(['Paid', 'On Payday', 'Pending']),
    workDoneQuantity: z.coerce.number().optional().nullable(),
    workDoneUnit: z.string().optional().nullable(),
    workers: z.array(workerCountSchema).min(1, "At least one worker type is required"),
});

const materialEntrySchema = z.object({
    projectMaterialId: z.string().optional(),
    materialName: z.string().min(1, "Material name is required"),
    quantityConsumed: z.coerce.number().min(0, "Quantity must be non-negative"),
    unit: z.string().optional(),
});

const photoEntrySchema = z.object({
    photoUrl: z.string().url(),
    caption: z.string().optional(),
});

const createWorklogSchema = z.object({
    title: z.string().min(1, "Title is required"),
    date: z.date({
        required_error: "A date is required.",
    }),
    labor: z.array(laborEntrySchema),
    materials: z.array(materialEntrySchema),
    photos: z.array(photoEntrySchema),
});

type CreateWorklogFormValues = z.infer<typeof createWorklogSchema>;

interface CreateWorklogDialogProps {
    projectId?: string;
    onSuccess?: () => void;
    trigger?: React.ReactNode;
    initialData?: any; // For editing
    worklogId?: string; // For editing
    forceOpen?: boolean;
}

export function CreateWorklogDialog({ projectId, onSuccess, trigger, initialData, worklogId, forceOpen }: CreateWorklogDialogProps) {
    const isEditing = !!initialData;
    const [open, setOpen] = useState(forceOpen || false);

    useEffect(() => {
        if (forceOpen !== undefined) {
            setOpen(forceOpen);
        }
    }, [forceOpen]);

    // State to track selected project ID (either from prop or selection)
    const [selectedProjectId, setSelectedProjectId] = useState<string | undefined>(projectId || initialData?.project_id);

    // Fetch projects for selection if no projectId prop is provided
    const { data: projects = [] } = useProjects();
    const [projectMaterials, setProjectMaterials] = useState<any[]>([]);
    const [contractors, setContractors] = useState<any[]>([]);
    const [isCreateContractorOpen, setIsCreateContractorOpen] = useState(false);
    const [activeLaborIndex, setActiveLaborIndex] = useState<number | null>(null);
    const [isUploading, setIsUploading] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);
    const [activeTab, setActiveTab] = useState("details");
    const { toast } = useToast();
    const { supabase } = useSupabase();

    // Effect to update selectedProjectId when prop changes
    useEffect(() => {
        if (projectId) setSelectedProjectId(projectId);
    }, [projectId]);

    // Initial values logic
    const defaultDate = initialData?.date ? new Date(initialData.date) : new Date();
    // Transform initial data if needed
    const defaultLabor = initialData?.labor?.map((l: any) => ({
        contractorName: l.contractor_name,
        category: l.category,
        workDescription: l.work_description,
        paymentStatus: l.payment_status,
        workDoneQuantity: l.work_done_quantity,
        workDoneUnit: l.work_done_unit,
        workers: l.workers?.map((w: any) => ({ workerType: w.worker_type, count: w.count })) || []
    })) || [];
    const defaultMaterials = initialData?.materials?.map((m: any) => ({
        projectMaterialId: m.project_material_id,
        materialName: m.material_name,
        quantityConsumed: m.quantity_consumed,
        unit: m.unit
    })) || [];
    const defaultPhotos = initialData?.photos?.map((p: any) => ({
        photoUrl: p.photo_url,
        caption: p.caption
    })) || [];

    const form = useForm<CreateWorklogFormValues>({
        resolver: zodResolver(createWorklogSchema),
        defaultValues: {
            title: initialData?.title || "",
            date: defaultDate,
            labor: defaultLabor,
            materials: defaultMaterials,
            photos: defaultPhotos,
        },
    });

    // Reset form when initialData changes or dialog opens/closes
    useEffect(() => {
        if (open) {
            form.reset({
                title: initialData?.title || "",
                date: defaultDate,
                labor: defaultLabor,
                materials: defaultMaterials,
                photos: defaultPhotos,
            });
            if (initialData?.project_id) setSelectedProjectId(initialData.project_id);
        }
    }, [open, initialData, form]);


    const { fields: laborFields, append: appendLabor, remove: removeLabor } = useFieldArray({
        control: form.control,
        name: "labor",
    });

    const { fields: materialFields, append: appendMaterial, remove: removeMaterial } = useFieldArray({
        control: form.control,
        name: "materials",
    });

    const { fields: photoFields, append: appendPhoto, remove: removePhoto } = useFieldArray({
        control: form.control,
        name: "photos",
    });

    useEffect(() => {
        if (open && selectedProjectId) {
            fetchData(selectedProjectId);
        }
    }, [open, selectedProjectId]);

    const fetchData = async (pId: string) => {
        const [materialsResult, contractorsResult] = await Promise.all([
            getProjectMaterials(pId),
            getContractors()
        ]);

        if (materialsResult.success && materialsResult.data) {
            setProjectMaterials(materialsResult.data);
        }
        if (contractorsResult.success && contractorsResult.data) {
            setContractors(contractorsResult.data);
        }
    };

    const onSubmit = async (data: CreateWorklogFormValues) => {
        setSubmitError(null);
        if (!selectedProjectId) {
            setSubmitError("Please select a project.");
            return;
        }

        try {
            const formattedData = {
                projectId: selectedProjectId,
                title: data.title,
                date: format(data.date, 'yyyy-MM-dd'),
                labor: data.labor,
                materials: data.materials,
                photos: data.photos,
            };

            let result;
            if (isEditing && worklogId) {
                result = await updateWorklog(worklogId, formattedData);
            } else {
                result = await createWorklog(formattedData);
            }

            if (result.success) {
                toast({
                    title: isEditing ? "Worklog updated" : "Worklog created",
                    description: isEditing ? "The daily worklog has been updated." : "The daily worklog has been successfully saved.",
                });
                setOpen(false);
                form.reset();
                setActiveTab("details");
                setSubmitError(null);
                if (!projectId && !isEditing) setSelectedProjectId(undefined);
                if (onSuccess) onSuccess();
            } else {
                const errorMessage = result.error || "Failed to save worklog.";
                setSubmitError(errorMessage);
                toast({ variant: "destructive", title: "Error", description: errorMessage });
            }
        } catch (error: any) {
            console.error(error);
            setSubmitError(error.message || "Something went wrong.");
        }
    };

    const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
        if (!selectedProjectId) return;

        const files = e.target.files;
        if (!files || files.length === 0) return;

        setIsUploading(true);
        try {
            for (const file of Array.from(files)) {
                // Compress image before upload
                let fileToUpload = file;
                try {
                    fileToUpload = await compressImage(file);
                } catch (err) {
                    console.warn("Compression failed, using original file", err);
                }

                const fileExt = fileToUpload.name.split('.').pop() || 'jpg';
                const fileName = `${Math.random().toString(36).substring(2)}.${fileExt}`;
                const filePath = `${selectedProjectId}/${fileName}`;

                const { error: uploadError } = await supabase.storage
                    .from('worklog_photos')
                    .upload(filePath, fileToUpload);

                if (uploadError) throw uploadError;

                const { data: { publicUrl } } = supabase.storage
                    .from('worklog_photos')
                    .getPublicUrl(filePath);

                appendPhoto({ photoUrl: publicUrl, caption: '' });
            }
        } catch (error: any) {
            toast({
                variant: "destructive",
                title: "Upload failed",
                description: error.message,
            });
        } finally {
            setIsUploading(false);
        }
    };

    // ... helper functions for tabs and contractors ...
    const nextTab = (current: string) => {
        if (!selectedProjectId) {
            toast({ title: "Select Project", description: "Please select a project to proceed." });
            return;
        }
        if (current === "details") setActiveTab("labor");
        if (current === "labor") setActiveTab("materials");
        if (current === "materials") setActiveTab("photos");
    };

    const handleContractorCreated = (newContractor: any) => {
        setContractors(prev => [...prev, newContractor]);
        if (activeLaborIndex !== null) {
            form.setValue(`labor.${activeLaborIndex}.contractorName`, newContractor.name);
            if (newContractor.category) form.setValue(`labor.${activeLaborIndex}.category`, newContractor.category);
            setActiveLaborIndex(null);
        }
    };

    return (
        <>
            <Dialog open={open} onOpenChange={setOpen}>
                <DialogTrigger asChild>
                    {trigger ? trigger : (
                        <Button size="lg" className="shadow-lg hover:shadow-xl transition-all w-full md:w-auto bg-gradient-to-r from-primary to-primary/90">
                            <Plus className="mr-2 h-5 w-5" />
                            Add Daily Log
                        </Button>
                    )}
                </DialogTrigger>
                <DialogContent className="w-screen h-[100dvh] max-w-none rounded-none sm:rounded-2xl sm:h-[90vh] sm:max-w-5xl flex flex-col p-0 gap-0 overflow-hidden glass border border-white/10 dark:border-white/5 shadow-2xl">
                    <div className="p-4 md:p-6 border-b border-white/10 dark:border-white/5 bg-white/5 dark:bg-black/20 flex items-center gap-4">
                        <Button variant="ghost" size="icon" className="md:hidden -ml-2 rounded-full hover:bg-white/10 dark:hover:bg-white/5 text-foreground" onClick={() => setOpen(false)}>
                            <ArrowLeft className="h-5 w-5" />
                        </Button>
                        <div className="flex-1">
                            <DialogHeader className="text-left space-y-1">
                                <DialogTitle className="text-xl md:text-2xl font-headline tracking-tight text-foreground">
                                    {isEditing ? 'Edit Daily Worklog' : 'New Daily Worklog'}
                                </DialogTitle>
                                <DialogDescription className="text-muted-foreground/80 flex items-center gap-2">
                                    <span className="hidden md:inline">Record site activity, labor, materials, and photos.</span>
                                </DialogDescription>
                            </DialogHeader>
                        </div>
                        <div className="flex items-center gap-2">
                            <Button variant="ghost" className="hidden md:flex hover:bg-white/10 dark:hover:bg-white/5 text-foreground" onClick={() => setOpen(false)}>Cancel</Button>
                        </div>
                    </div>

                    <Form {...form}>
                        <form onSubmit={form.handleSubmit(onSubmit, (errors) => {
                            console.error("Form Validation Errors:", JSON.stringify(errors, null, 2));
                            toast({ variant: "destructive", title: "Validation Error", description: "Please check all tabs for missing or invalid fields." });
                        })} className="flex-1 flex flex-col overflow-hidden">
                            <div className="flex-1 overflow-y-auto">
                                <Tabs value={activeTab} onValueChange={(val) => selectedProjectId ? setActiveTab(val) : null} className="h-full flex flex-col">
                                    <div className="px-4 md:px-8 py-2 border-b border-white/10 dark:border-white/5 bg-transparent sticky top-0 z-20">
                                        <TabsList className="grid w-full grid-cols-4 h-12 bg-white/5 dark:bg-black/20 border border-white/10 dark:border-white/5 p-1 rounded-xl">
                                            <TabsTrigger value="details" className="gap-2 data-[state=active]:bg-white/15 dark:data-[state=active]:bg-white/10 data-[state=active]:shadow-sm transition-all text-xs md:text-sm data-[state=active]:text-foreground text-muted-foreground">
                                                <CalendarLucide className="h-4 w-4" />
                                                <span className="hidden sm:inline font-medium">Details</span>
                                            </TabsTrigger>
                                            <TabsTrigger value="labor" disabled={!selectedProjectId} className="gap-2 data-[state=active]:bg-white/15 dark:data-[state=active]:bg-white/10 data-[state=active]:shadow-sm transition-all text-xs md:text-sm data-[state=active]:text-foreground text-muted-foreground">
                                                <Users className="h-4 w-4" />
                                                <span className="hidden sm:inline font-medium">Labor</span>
                                                {laborFields.length > 0 && <Badge variant="secondary" className="ml-1 px-1.5 h-5 min-w-[1.25rem] bg-white/20 dark:bg-white/10 text-foreground">{laborFields.length}</Badge>}
                                            </TabsTrigger>
                                            <TabsTrigger value="materials" disabled={!selectedProjectId} className="gap-2 data-[state=active]:bg-white/15 dark:data-[state=active]:bg-white/10 data-[state=active]:shadow-sm transition-all text-xs md:text-sm data-[state=active]:text-foreground text-muted-foreground">
                                                <Package className="h-4 w-4" />
                                                <span className="hidden sm:inline font-medium">Materials</span>
                                                {materialFields.length > 0 && <Badge variant="secondary" className="ml-1 px-1.5 h-5 min-w-[1.25rem] bg-white/20 dark:bg-white/10 text-foreground">{materialFields.length}</Badge>}
                                            </TabsTrigger>
                                            <TabsTrigger value="photos" disabled={!selectedProjectId} className="gap-2 data-[state=active]:bg-white/15 dark:data-[state=active]:bg-white/10 data-[state=active]:shadow-sm transition-all text-xs md:text-sm data-[state=active]:text-foreground text-muted-foreground">
                                                <ImageIcon className="h-4 w-4" />
                                                <span className="hidden sm:inline font-medium">Photos</span>
                                                {photoFields.length > 0 && <Badge variant="secondary" className="ml-1 px-1.5 h-5 min-w-[1.25rem] bg-white/20 dark:bg-white/10 text-foreground">{photoFields.length}</Badge>}
                                            </TabsTrigger>
                                        </TabsList>
                                    </div>

                                    <div className="p-4 md:p-8 flex-1 max-w-5xl mx-auto w-full">
                                        <TabsContent value="details" className="mt-0 space-y-8 h-full animate-in fade-in-50 slide-in-from-right-2 duration-300">
                                            <div className="max-w-lg mx-auto space-y-6 pt-2">
                                                <div className="space-y-6 glass-card p-6 shadow-sm bg-transparent border-white/10 dark:border-white/5">
                                                    <div className="space-y-2">
                                                        <label className="text-sm font-medium leading-none text-foreground">Project</label>
                                                        <Select
                                                            value={selectedProjectId}
                                                            onValueChange={(val) => {
                                                                setSelectedProjectId(val);
                                                                if (val !== selectedProjectId) {
                                                                    form.setValue('labor', []);
                                                                    form.setValue('materials', []);
                                                                    setProjectMaterials([]);
                                                                    fetchData(val);
                                                                }
                                                            }}
                                                            disabled={isEditing} // Lock project if editing
                                                        >
                                                            <SelectTrigger className="h-12 text-base glass border-white/10 dark:border-white/5 bg-transparent text-foreground">
                                                                <SelectValue placeholder="Select a project..." />
                                                            </SelectTrigger>
                                                            <SelectContent className="glass border-white/10 dark:border-white/5 text-foreground">
                                                                {projects.map(project => (
                                                                    <SelectItem key={project.id} value={project.id} className="focus:bg-white/10 dark:focus:bg-white/5">{project.name}</SelectItem>
                                                                ))}
                                                            </SelectContent>
                                                        </Select>
                                                    </div>

                                                    <FormField
                                                        control={form.control}
                                                        name="title"
                                                        render={({ field }) => (
                                                            <FormItem>
                                                                <FormLabel className="text-foreground">Work Title</FormLabel>
                                                                <FormControl>
                                                                    <Input {...field} value={field.value as string} placeholder="e.g. Foundation Pouring" className="h-12 text-base glass border-white/10 dark:border-white/5 bg-transparent focus-visible:ring-primary text-foreground" />
                                                                </FormControl>
                                                                <FormMessage />
                                                            </FormItem>
                                                        )}
                                                    />

                                                    <FormField
                                                        control={form.control}
                                                        name="date"
                                                        render={({ field }) => (
                                                            <FormItem className="flex flex-col">
                                                                <FormLabel>Date</FormLabel>
                                                                <Popover>
                                                                    <PopoverTrigger asChild>
                                                                         <FormControl>
                                                                             <Button
                                                                                 variant={"outline"}
                                                                                 className={cn(
                                                                                     "w-full h-12 pl-4 text-left font-normal text-base justify-start glass border-white/10 dark:border-white/5 bg-transparent hover:bg-white/10 dark:hover:bg-white/5 text-foreground",
                                                                                     !field.value && "text-muted-foreground"
                                                                                 )}
                                                                             >
                                                                                 <CalendarIcon className="mr-2 h-5 w-5 opacity-50" />
                                                                                 {field.value ? format(field.value, "PPP") : <span>Pick a date</span>}
                                                                             </Button>
                                                                         </FormControl>
                                                                     </PopoverTrigger>
                                                                     <PopoverContent className="w-auto p-0 glass border-white/10 dark:border-white/5" align="center">
                                                                         <Calendar
                                                                             mode="single"
                                                                             selected={field.value}
                                                                             onSelect={field.onChange}
                                                                             disabled={(date) => date > new Date() || date < new Date("1900-01-01")}
                                                                             initialFocus
                                                                             className="bg-transparent"
                                                                         />
                                                                     </PopoverContent>
                                                                 </Popover>
                                                                 <FormMessage />
                                                             </FormItem>
                                                         )}
                                                     />
                                                </div>
                                                <Button type="button" onClick={() => nextTab("details")} className="w-full h-12 text-base shadow-md hover:shadow-lg transition-all glass border-white/10 dark:border-white/5 hover:bg-white/10 dark:hover:bg-white/5 text-foreground" disabled={!selectedProjectId}>
                                                    Continue to Labor
                                                </Button>
                                            </div>
                                        </TabsContent>

                                        <TabsContent value="labor" className="mt-0 space-y-6 animate-in fade-in-50 slide-in-from-right-2 duration-300">
                                             <div className="flex justify-between items-center glass p-4 rounded-2xl border border-white/10 dark:border-white/5 shadow-sm bg-transparent">
                                                 <div><h3 className="text-lg font-semibold text-foreground">Labor Teams</h3><p className="text-sm text-muted-foreground">Who worked on site today?</p></div>
                                                 <Button type="button" onClick={() => appendLabor({ contractorName: "", category: "", paymentStatus: "Pending", workDoneQuantity: null, workDoneUnit: "", workers: [{ workerType: "Mason", count: 1 }] })} className="shadow-sm glass border-white/10 dark:border-white/5 hover:bg-white/10 dark:hover:bg-white/5"><Plus className="mr-2 h-4 w-4" />Add Team</Button>
                                             </div>
                                             {laborFields.length === 0 ? (
                                                 <div className="flex flex-col items-center justify-center py-16 border border-white/10 dark:border-white/5 rounded-2xl glass-card bg-transparent">
                                                     <div className="w-16 h-16 bg-white/5 dark:bg-black/20 rounded-full flex items-center justify-center mb-4"><Users className="h-8 w-8 text-muted-foreground" /></div>
                                                     <h3 className="text-lg font-medium text-foreground">No labor entries yet</h3>
                                                     <Button variant="outline" className="mt-4 glass border-white/10 dark:border-white/5 hover:bg-white/10 dark:hover:bg-white/5" onClick={() => appendLabor({ contractorName: "", category: "", paymentStatus: "Pending", workDoneQuantity: null, workDoneUnit: "", workers: [{ workerType: "Mason", count: 1 }] })}>Add First Team</Button>
                                                 </div>
                                             ) : (
                                                 <div className="grid gap-6">{laborFields.map((field, index) => <LaborEntryForm key={field.id} index={index} form={form} remove={() => removeLabor(index)} contractors={contractors} onAddNew={() => { setActiveLaborIndex(index); setIsCreateContractorOpen(true); }} />)}</div>
                                             )}
                                             <div className="flex justify-end pt-6"><Button type="button" size="lg" onClick={() => nextTab("labor")} className="px-8 glass border-white/10 dark:border-white/5 hover:bg-white/10 dark:hover:bg-white/5 text-foreground">Continue to Materials</Button></div>
                                        </TabsContent>

                                        <TabsContent value="materials" className="mt-0 space-y-6 animate-in fade-in-50 slide-in-from-right-2 duration-300">
                                             <div className="flex justify-between items-center glass p-4 rounded-2xl border border-white/10 dark:border-white/5 shadow-sm bg-transparent">
                                                 <div><h3 className="text-lg font-semibold text-foreground">Materials</h3><p className="text-sm text-muted-foreground">What was consumed from inventory?</p></div>
                                                 <Button type="button" onClick={() => appendMaterial({ materialName: "", quantityConsumed: 0, unit: "" })} className="shadow-sm glass border-white/10 dark:border-white/5 hover:bg-white/10 dark:hover:bg-white/5"><Plus className="mr-2 h-4 w-4" />Add Log</Button>
                                             </div>
                                             {materialFields.length === 0 ? <div className="text-center py-12 border border-white/10 dark:border-white/5 rounded-2xl glass bg-transparent text-muted-foreground">No materials logged today</div> :
                                                 <div className="space-y-4">{materialFields.map((field, index) => <MaterialEntryForm key={field.id} index={index} form={form} remove={() => removeMaterial(index)} materials={projectMaterials} />)}</div>
                                             }
                                             <div className="flex justify-end pt-6"><Button type="button" size="lg" onClick={() => nextTab("materials")} className="px-8 glass border-white/10 dark:border-white/5 hover:bg-white/10 dark:hover:bg-white/5 text-foreground">Continue to Photos</Button></div>
                                        </TabsContent>

                                        <TabsContent value="photos" className="mt-0 space-y-6 animate-in fade-in-50 slide-in-from-right-2 duration-300">
                                             <div className="flex justify-between items-center glass p-4 rounded-2xl border border-white/10 dark:border-white/5 shadow-sm bg-transparent">
                                                 <div><h3 className="text-lg font-semibold text-foreground">Site Photos</h3><p className="text-sm text-muted-foreground">Upload site progress images.</p></div>
                                                 <div className="flex items-center gap-2"><Input type="file" accept="image/*" multiple className="hidden" id="photo-upload" onChange={handleFileUpload} disabled={isUploading} /><Button type="button" disabled={isUploading} onClick={() => document.getElementById('photo-upload')?.click()} size="sm" className="shadow-sm glass border-white/10 dark:border-white/5 hover:bg-white/10 dark:hover:bg-white/5 text-foreground">{isUploading ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Upload className="mr-2 h-4 w-4" />}Upload</Button></div>
                                             </div>
                                             {photoFields.length === 0 ? <div className="text-center py-12 border border-white/10 dark:border-white/5 rounded-2xl glass bg-transparent text-muted-foreground cursor-pointer hover:bg-white/5" onClick={() => document.getElementById('photo-upload')?.click()}>Click to upload photos</div> :
                                                 <div className="grid grid-cols-2 md:grid-cols-4 gap-4">{photoFields.map((field, index) => <PhotoEntryForm key={field.id} index={index} form={form} remove={() => removePhoto(index)} />)}</div>
                                             }
                                        </TabsContent>
                                    </div>
                                </Tabs>
                            </div>

                             <div className="p-4 md:p-6 border-t border-white/10 dark:border-white/5 bg-white/5 dark:bg-black/20 backdrop-blur-md flex flex-col md:flex-row justify-between items-center gap-4 sticky bottom-0 z-20">
                                 <div className="text-xs md:text-sm text-muted-foreground text-center md:text-left font-medium">
                                     Summary: {laborFields.length} teams • {materialFields.length} materials • {photoFields.length} photos
                                 </div>
                                 <div className="flex flex-col items-end gap-2 w-full md:w-auto">
                                     {submitError && <p className="text-sm text-destructive font-medium px-2 py-1 bg-destructive/10 rounded-md">{submitError}</p>}
                                     <div className="flex gap-3 w-full md:w-auto">
                                         <Button type="button" variant="outline" onClick={() => setOpen(false)} className="flex-1 md:flex-none glass border-white/10 dark:border-white/5 hover:bg-white/10 dark:hover:bg-white/5 text-foreground">Cancel</Button>
                                         <Button type="submit" size="lg" disabled={form.formState.isSubmitting || !selectedProjectId} className="flex-1 md:flex-none min-w-[160px] shadow-lg bg-primary hover:bg-primary/95 text-white">
                                             {form.formState.isSubmitting ? <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> {isEditing ? 'Updating...' : 'Saving...'}</> : <><CheckCircle2 className="mr-2 h-5 w-5" /> {isEditing ? 'Update Worklog' : 'Save Worklog'}</>}
                                         </Button>
                                     </div>
                                 </div>
                             </div>
                        </form>
                    </Form>
                </DialogContent>
            </Dialog>

            <CreateContractorDialog open={isCreateContractorOpen} onOpenChange={setIsCreateContractorOpen} onSuccess={handleContractorCreated} />
        </>
    );
}

function LaborEntryForm({ index, form, remove, contractors, onAddNew }: any) {
    const { fields: workerFields, append: appendWorker, remove: removeWorker } = useFieldArray({
        control: form.control,
        name: `labor.${index}.workers`
    });

    const presetCategories = ["Mason", "MC", "FC", "Helper", "Supervisor", "Electrician", "Plumber", "Carpenter"];

    return (
        <div className="overflow-hidden border-l-4 border-l-primary/50 glass-card bg-transparent rounded-2xl border-t border-r border-b border-white/10 dark:border-white/5">
            <div className="p-4 space-y-4">
                <div className="flex flex-col md:flex-row gap-4">
                    <FormField control={form.control} name={`labor.${index}.contractorName`} render={({ field }) => (
                        <FormItem className="flex-1 min-w-[200px]">
                            <FormLabel className="text-foreground">Contractor / Team</FormLabel>
                            <Select onValueChange={(val) => {
                                if (val === 'new') onAddNew();
                                else {
                                    field.onChange(val);
                                    const c = contractors.find((c: any) => c.name === val);
                                    if (c?.category) form.setValue(`labor.${index}.category`, c.category);
                                }
                            }} value={field.value}>
                                <FormControl><SelectTrigger className="glass border-white/10 dark:border-white/5 bg-transparent text-foreground"><SelectValue placeholder="Select contractor" /></SelectTrigger></FormControl>
                                <SelectContent className="glass border-white/10 dark:border-white/5 text-foreground">
                                    <SelectItem value="new" className="text-primary font-medium cursor-pointer focus:bg-white/10">+ Add New Contractor</SelectItem>
                                    {contractors.map((c: any) => <SelectItem key={c.id} value={c.name} className="focus:bg-white/10 dark:focus:bg-white/5">{c.name}</SelectItem>)}
                                </SelectContent>
                            </Select>
                            <FormMessage />
                        </FormItem>
                    )} />
                    <FormField control={form.control} name={`labor.${index}.category`} render={({ field }) => (
                        <FormItem className="flex-1"><FormLabel className="text-foreground">Category</FormLabel><FormControl><Input {...field} placeholder="e.g. Masonry" className="glass border-white/10 dark:border-white/5 bg-transparent focus-visible:ring-primary text-foreground" /></FormControl><FormMessage /></FormItem>
                    )} />
                    <FormField control={form.control} name={`labor.${index}.paymentStatus`} render={({ field }) => (
                        <FormItem className="w-full md:w-32"><FormLabel className="text-foreground">Payment</FormLabel><Select onValueChange={field.onChange} value={field.value} defaultValue="Pending"><FormControl><SelectTrigger className={cn("glass border-white/10 dark:border-white/5 bg-transparent", field.value === 'Paid' ? "text-green-500" : field.value === 'Pending' ? "text-orange-500" : "text-foreground")}><SelectValue /></SelectTrigger></FormControl><SelectContent className="glass border-white/10 dark:border-white/5 text-foreground"><SelectItem value="Pending" className="focus:bg-white/10 dark:focus:bg-white/5">Pending</SelectItem><SelectItem value="On Payday" className="focus:bg-white/10 dark:focus:bg-white/5">On Payday</SelectItem><SelectItem value="Paid" className="focus:bg-white/10 dark:focus:bg-white/5">Paid</SelectItem></SelectContent></Select><FormMessage /></FormItem>
                    )} />
                </div>
                <FormField control={form.control} name={`labor.${index}.workDescription`} render={({ field }) => (
                    <FormItem><FormLabel className="text-foreground">Description / Scope of Work</FormLabel><FormControl><Textarea {...field} placeholder="What did they work on today?" className="glass border-white/10 dark:border-white/5 bg-transparent resize-none focus-visible:ring-primary text-foreground" rows={2} /></FormControl><FormMessage /></FormItem>
                )} />
                <div className="flex flex-col sm:flex-row gap-4">
                    <FormField control={form.control} name={`labor.${index}.workDoneQuantity`} render={({ field }) => (
                        <FormItem className="flex-1">
                            <FormLabel className="text-foreground">Work Done Quantity</FormLabel>
                            <FormControl>
                                <Input type="number" step="any" {...field} value={field.value ?? ""} placeholder="e.g. 500" className="h-10 text-sm glass border-white/10 dark:border-white/5 bg-transparent focus-visible:ring-primary text-foreground" />
                            </FormControl>
                            <FormMessage />
                        </FormItem>
                    )} />
                    <FormField control={form.control} name={`labor.${index}.workDoneUnit`} render={({ field }) => (
                        <FormItem className="flex-1">
                            <FormLabel className="text-foreground">Work Done Unit</FormLabel>
                            <FormControl>
                                <Input {...field} value={field.value ?? ""} placeholder="e.g. sq ft, cum, running meters" className="h-10 text-sm glass border-white/10 dark:border-white/5 bg-transparent focus-visible:ring-primary text-foreground" />
                            </FormControl>
                            <FormMessage />
                        </FormItem>
                    )} />
                </div>
                
                {/* Workers Array section */}
                <div className="space-y-3 pt-2">
                    <div className="flex justify-between items-center">
                        <h4 className="text-sm font-semibold text-foreground flex items-center gap-1.5">
                            <Users className="h-4 w-4 text-primary" />
                            Worker Counts <span className="text-xs font-normal text-muted-foreground">(At least one required)</span>
                        </h4>
                        <Button 
                            type="button" 
                            variant="outline" 
                            size="sm" 
                            onClick={() => appendWorker({ workerType: "Mason", count: 1 })}
                            className="h-8 glass border-white/10 dark:border-white/5 hover:bg-white/10 dark:hover:bg-white/5"
                        >
                            <Plus className="mr-1.5 h-3.5 w-3.5" />
                            Add Category
                        </Button>
                    </div>

                    {workerFields.length === 0 ? (
                        <p className="text-xs text-orange-500 font-medium">Please add at least one worker type and count.</p>
                    ) : (
                        <div className="space-y-3">
                            {workerFields.map((workerField, wIndex) => {
                                const currentWorkerType = form.watch(`labor.${index}.workers.${wIndex}.workerType`);
                                const isPreset = presetCategories.includes(currentWorkerType);
                                
                                return (
                                    <div key={workerField.id} className="flex flex-col sm:flex-row gap-3 items-start sm:items-end glass-card p-3 border-white/5 bg-white/5 rounded-xl">
                                        <FormField 
                                            control={form.control}
                                            name={`labor.${index}.workers.${wIndex}.workerType`}
                                            render={({ field }) => (
                                                <FormItem className="w-full sm:w-48">
                                                    <FormLabel className="text-xs text-muted-foreground">Category</FormLabel>
                                                    <Select
                                                        value={isPreset ? field.value : (field.value ? "Custom" : "")}
                                                        onValueChange={(val) => {
                                                            if (val === "Custom") {
                                                                field.onChange(""); // Reset to empty so they can type it
                                                            } else {
                                                                field.onChange(val);
                                                            }
                                                        }}
                                                    >
                                                        <FormControl>
                                                            <SelectTrigger className="h-9 text-sm glass border-white/10 dark:border-white/5 bg-transparent text-foreground">
                                                                <SelectValue placeholder="Select category" />
                                                            </SelectTrigger>
                                                        </FormControl>
                                                        <SelectContent className="glass border-white/10 dark:border-white/5 text-foreground">
                                                            {presetCategories.map(cat => (
                                                                <SelectItem key={cat} value={cat} className="focus:bg-white/10 dark:focus:bg-white/5">{cat}</SelectItem>
                                                            ))}
                                                            <SelectItem value="Custom" className="focus:bg-white/10 dark:focus:bg-white/5 font-medium text-primary">Custom...</SelectItem>
                                                        </SelectContent>
                                                    </Select>
                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />

                                        {!isPreset && (
                                            <FormField 
                                                control={form.control}
                                                name={`labor.${index}.workers.${wIndex}.workerType`}
                                                render={({ field }) => (
                                                    <FormItem className="w-full sm:flex-1">
                                                        <FormLabel className="text-xs text-muted-foreground">Custom Name</FormLabel>
                                                        <FormControl>
                                                            <Input 
                                                                {...field}
                                                                placeholder="e.g. Bar Bender" 
                                                                className="h-9 text-sm glass border-white/10 dark:border-white/5 bg-transparent text-foreground focus-visible:ring-primary"
                                                            />
                                                        </FormControl>
                                                        <FormMessage />
                                                    </FormItem>
                                                )}
                                            />
                                        )}

                                        <FormField 
                                            control={form.control}
                                            name={`labor.${index}.workers.${wIndex}.count`}
                                            render={({ field }) => (
                                                <FormItem className="w-24 sm:w-20">
                                                    <FormLabel className="text-xs text-muted-foreground">Count</FormLabel>
                                                    <FormControl>
                                                        <Input 
                                                            type="number"
                                                            {...field}
                                                            className="h-9 text-sm glass border-white/10 dark:border-white/5 bg-transparent text-foreground focus-visible:ring-primary"
                                                        />
                                                    </FormControl>
                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />

                                        <Button 
                                            type="button" 
                                            variant="ghost" 
                                            size="icon" 
                                            onClick={() => removeWorker(wIndex)}
                                            className="text-destructive hover:bg-destructive/10 h-9 w-9"
                                        >
                                            <Trash2 className="h-4 w-4" />
                                        </Button>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>

                <div className="flex justify-between items-center pt-2 border-t border-white/5">
                    <div className="text-xs text-muted-foreground">
                        {workerFields.length > 0 ? (
                            <span>
                                Total Workers:{" "}
                                {workerFields.reduce((sum, f, wIdx) => {
                                    const c = Number(form.watch(`labor.${index}.workers.${wIdx}.count`) || 0);
                                    return sum + c;
                                }, 0)}
                            </span>
                        ) : null}
                    </div>
                    <Button type="button" variant="ghost" size="sm" onClick={remove} className="text-destructive hover:bg-destructive/10">
                        <Trash2 className="mr-2 h-4 w-4" />Remove Team
                    </Button>
                </div>
            </div>
        </div>
    );
}

function MaterialEntryForm({ index, form, remove, materials }: any) {
    return (
        <div className="overflow-hidden border-l-4 border-l-orange-500/50 glass-card bg-transparent rounded-2xl border-t border-r border-b border-white/10 dark:border-white/5">
            <div className="p-4 flex flex-col md:flex-row gap-4 items-end">
                <FormField control={form.control} name={`materials.${index}.projectMaterialId`} render={({ field }) => (
                    <FormItem className="w-full md:flex-1"><FormLabel className="text-foreground">Select Material</FormLabel><Select onValueChange={(val) => { field.onChange(val); const m = materials.find((mat: any) => mat.id === val); if (m) { form.setValue(`materials.${index}.materialName`, m.name); form.setValue(`materials.${index}.unit`, m.unit); } }} value={field.value}><FormControl><SelectTrigger className="glass border-white/10 dark:border-white/5 bg-transparent text-foreground"><SelectValue placeholder="Inventory" /></SelectTrigger></FormControl><SelectContent className="glass border-white/10 dark:border-white/5 text-foreground">{materials.map((m: any) => <SelectItem key={m.id} value={m.id} className="focus:bg-white/10 dark:focus:bg-white/5">{m.name}</SelectItem>)}</SelectContent></Select></FormItem>
                )} />
                <FormField control={form.control} name={`materials.${index}.materialName`} render={({ field }) => (
                    <FormItem className="w-full md:flex-1"><FormLabel className="text-foreground">Name</FormLabel><FormControl><Input {...field} placeholder="Name" className="glass border-white/10 dark:border-white/5 bg-transparent focus-visible:ring-primary text-foreground" /></FormControl></FormItem>
                )} />
                <FormField control={form.control} name={`materials.${index}.quantityConsumed`} render={({ field }) => (
                    <FormItem className="flex-1 md:w-24"><FormLabel className="text-foreground">Qty</FormLabel><FormControl><Input type="number" {...field} className="glass border-white/10 dark:border-white/5 bg-transparent focus-visible:ring-primary text-foreground" /></FormControl></FormItem>
                )} />
                <FormField control={form.control} name={`materials.${index}.unit`} render={({ field }) => (
                    <FormItem className="flex-1 md:w-24"><FormLabel className="text-foreground">Unit</FormLabel><FormControl><Input {...field} className="glass border-white/10 dark:border-white/5 bg-transparent focus-visible:ring-primary text-foreground" /></FormControl></FormItem>
                )} />
                <Button type="button" variant="ghost" size="icon" onClick={remove} className="text-destructive"><Trash2 className="h-5 w-5" /></Button>
            </div>
        </div>
    )
}

function PhotoEntryForm({ index, form, remove }: any) {
    return (
        <div className="relative group border border-white/10 dark:border-white/5 rounded-xl overflow-hidden shadow-sm glass bg-transparent transition-all hover:shadow-md">
            <FormField control={form.control} name={`photos.${index}.photoUrl`} render={({ field }) => (
                <div className="aspect-square relative"><img src={field.value} alt="Worklog" className="w-full h-full object-cover" /><div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center"><Button type="button" variant="destructive" size="icon" onClick={remove} className="rounded-full"><Trash2 className="h-4 w-4" /></Button></div></div>
            )} />
            <div className="p-2 bg-white/5 dark:bg-black/25"><FormField control={form.control} name={`photos.${index}.caption`} render={({ field }) => <Input {...field} placeholder="Caption..." className="h-8 text-xs border-0 bg-transparent text-foreground placeholder:text-muted-foreground/60 focus-visible:ring-0" />} /></div>
        </div>
    )
}
