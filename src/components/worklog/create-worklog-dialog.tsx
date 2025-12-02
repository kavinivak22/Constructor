'use client';

import { useState, useEffect } from 'react';
import { useForm, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { format } from 'date-fns';
import { CalendarIcon, Plus, Trash2, Loader2, Upload } from 'lucide-react';
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
import { createWorklog } from '@/app/actions/worklogs';
import { getProjectMaterials } from '@/app/actions/materials';
import { useSupabase } from '@/supabase/provider';

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
    date: z.date({
        required_error: "A date is required.",
    }),
    labor: z.array(laborEntrySchema),
    materials: z.array(materialEntrySchema),
    photos: z.array(photoEntrySchema),
});

type CreateWorklogFormValues = z.infer<typeof createWorklogSchema>;

interface CreateWorklogDialogProps {
    projectId: string;
    onSuccess?: () => void;
}

export function CreateWorklogDialog({ projectId, onSuccess }: CreateWorklogDialogProps) {
    const [open, setOpen] = useState(false);
    const [projectMaterials, setProjectMaterials] = useState<any[]>([]);
    const [isUploading, setIsUploading] = useState(false);
    const { toast } = useToast();
    const { supabase } = useSupabase();

    const form = useForm<CreateWorklogFormValues>({
        resolver: zodResolver(createWorklogSchema),
        defaultValues: {
            date: new Date(),
            labor: [],
            materials: [],
            photos: [],
        },
    });

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
        if (open && projectId) {
            getProjectMaterials(projectId).then((res) => {
                if (res.success && res.data) {
                    setProjectMaterials(res.data);
                }
            });
        }
    }, [open, projectId]);

    const onSubmit = async (data: CreateWorklogFormValues) => {
        try {
            const formattedData = {
                projectId,
                date: format(data.date, 'yyyy-MM-dd'),
                labor: data.labor,
                materials: data.materials,
                photos: data.photos,
            };

            const result = await createWorklog(formattedData);

            if (result.success) {
                toast({
                    title: "Worklog created",
                    description: "The daily worklog has been successfully saved.",
                });
                setOpen(false);
                form.reset();
                if (onSuccess) onSuccess();
            } else {
                toast({
                    variant: "destructive",
                    title: "Error",
                    description: result.error || "Failed to create worklog.",
                });
            }
        } catch (error) {
            console.error(error);
            toast({
                variant: "destructive",
                title: "Error",
                description: "Something went wrong.",
            });
        }
    };

    const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = e.target.files;
        if (!files || files.length === 0) return;

        setIsUploading(true);
        try {
            for (const file of Array.from(files)) {
                const fileExt = file.name.split('.').pop();
                const fileName = `${Math.random().toString(36).substring(2)}.${fileExt}`;
                const filePath = `${projectId}/${fileName}`;

                const { error: uploadError } = await supabase.storage
                    .from('worklog_photos')
                    .upload(filePath, file);

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

    return (
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
                <Button>
                    <Plus className="mr-2 h-4 w-4" />
                    Add Worklog
                </Button>
            </DialogTrigger>
            <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
                <DialogHeader>
                    <DialogTitle>Create Daily Worklog</DialogTitle>
                    <DialogDescription>
                        Log labor, materials, and photos for the day.
                    </DialogDescription>
                </DialogHeader>

                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8">

                        {/* Date Selection */}
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
                                                        "w-[240px] pl-3 text-left font-normal",
                                                        !field.value && "text-muted-foreground"
                                                    )}
                                                >
                                                    {field.value ? (
                                                        format(field.value, "PPP")
                                                    ) : (
                                                        <span>Pick a date</span>
                                                    )}
                                                    <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                                                </Button>
                                            </FormControl>
                                        </PopoverTrigger>
                                        <PopoverContent className="w-auto p-0" align="start">
                                            <Calendar
                                                mode="single"
                                                selected={field.value}
                                                onSelect={field.onChange}
                                                disabled={(date) =>
                                                    date > new Date() || date < new Date("1900-01-01")
                                                }
                                                initialFocus
                                            />
                                        </PopoverContent>
                                    </Popover>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        {/* Labor Section */}
                        <div className="space-y-4">
                            <div className="flex items-center justify-between">
                                <h3 className="text-lg font-medium">Labor</h3>
                                <Button
                                    type="button"
                                    variant="outline"
                                    size="sm"
                                    onClick={() => appendLabor({
                                        contractorName: "",
                                        category: "",
                                        paymentStatus: "Pending",
                                        workers: [{ workerType: "", count: 0 }]
                                    })}
                                >
                                    <Plus className="mr-2 h-4 w-4" />
                                    Add Team/Contractor
                                </Button>
                            </div>

                            {laborFields.map((field, index) => (
                                <LaborEntryForm
                                    key={field.id}
                                    index={index}
                                    form={form}
                                    remove={() => removeLabor(index)}
                                />
                            ))}
                        </div>

                        {/* Materials Section */}
                        <div className="space-y-4">
                            <div className="flex items-center justify-between">
                                <h3 className="text-lg font-medium">Materials</h3>
                                <Button
                                    type="button"
                                    variant="outline"
                                    size="sm"
                                    onClick={() => appendMaterial({
                                        materialName: "",
                                        quantityConsumed: 0,
                                        unit: "",
                                    })}
                                >
                                    <Plus className="mr-2 h-4 w-4" />
                                    Add Material
                                </Button>
                            </div>

                            {materialFields.map((field, index) => (
                                <div key={field.id} className="flex gap-4 items-end border p-4 rounded-md bg-muted/20">
                                    <FormField
                                        control={form.control}
                                        name={`materials.${index}.projectMaterialId`}
                                        render={({ field }) => (
                                            <FormItem className="flex-1">
                                                <FormLabel>Select Material</FormLabel>
                                                <Select
                                                    onValueChange={(value) => {
                                                        field.onChange(value);
                                                        const mat = projectMaterials.find(m => m.id === value);
                                                        if (mat) {
                                                            form.setValue(`materials.${index}.materialName`, mat.name);
                                                            form.setValue(`materials.${index}.unit`, mat.unit || '');
                                                        }
                                                    }}
                                                    value={field.value}
                                                >
                                                    <FormControl>
                                                        <SelectTrigger>
                                                            <SelectValue placeholder="Select from inventory" />
                                                        </SelectTrigger>
                                                    </FormControl>
                                                    <SelectContent>
                                                        {projectMaterials.map((m) => (
                                                            <SelectItem key={m.id} value={m.id}>
                                                                {m.name} (Qty: {m.quantity} {m.unit})
                                                            </SelectItem>
                                                        ))}
                                                    </SelectContent>
                                                </Select>
                                            </FormItem>
                                        )}
                                    />

                                    <FormField
                                        control={form.control}
                                        name={`materials.${index}.materialName`}
                                        render={({ field }) => (
                                            <FormItem className="flex-1">
                                                <FormLabel>Material Name</FormLabel>
                                                <FormControl>
                                                    <Input {...field} placeholder="Or type manually" />
                                                </FormControl>
                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />

                                    <FormField
                                        control={form.control}
                                        name={`materials.${index}.quantityConsumed`}
                                        render={({ field }) => (
                                            <FormItem className="w-24">
                                                <FormLabel>Qty</FormLabel>
                                                <FormControl>
                                                    <Input type="number" {...field} />
                                                </FormControl>
                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />

                                    <FormField
                                        control={form.control}
                                        name={`materials.${index}.unit`}
                                        render={({ field }) => (
                                            <FormItem className="w-24">
                                                <FormLabel>Unit</FormLabel>
                                                <FormControl>
                                                    <Input {...field} />
                                                </FormControl>
                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />

                                    <Button type="button" variant="ghost" size="icon" onClick={() => removeMaterial(index)}>
                                        <Trash2 className="h-4 w-4 text-destructive" />
                                    </Button>
                                </div>
                            ))}
                        </div>

                        {/* Photos Section */}
                        <div className="space-y-4">
                            <div className="flex items-center justify-between">
                                <h3 className="text-lg font-medium">Photos</h3>
                                <div className="flex items-center gap-2">
                                    <Input
                                        type="file"
                                        accept="image/*"
                                        multiple
                                        className="hidden"
                                        id="photo-upload"
                                        onChange={handleFileUpload}
                                        disabled={isUploading}
                                    />
                                    <Button
                                        type="button"
                                        variant="outline"
                                        size="sm"
                                        disabled={isUploading}
                                        onClick={() => document.getElementById('photo-upload')?.click()}
                                    >
                                        {isUploading ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Upload className="mr-2 h-4 w-4" />}
                                        Upload Photos
                                    </Button>
                                </div>
                            </div>

                            <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                                {photoFields.map((field, index) => (
                                    <div key={field.id} className="relative group border rounded-md overflow-hidden">
                                        <img src={field.photoUrl} alt="Worklog" className="w-full h-32 object-cover" />
                                        <Button
                                            type="button"
                                            variant="destructive"
                                            size="icon"
                                            className="absolute top-1 right-1 h-6 w-6 opacity-0 group-hover:opacity-100 transition-opacity"
                                            onClick={() => removePhoto(index)}
                                        >
                                            <Trash2 className="h-3 w-3" />
                                        </Button>
                                        <FormField
                                            control={form.control}
                                            name={`photos.${index}.caption`}
                                            render={({ field }) => (
                                                <Input {...field} placeholder="Caption" className="border-0 rounded-none focus-visible:ring-0" />
                                            )}
                                        />
                                    </div>
                                ))}
                            </div>
                        </div>

                        <Button type="submit" disabled={form.formState.isSubmitting} className="w-full">
                            {form.formState.isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                            Save Worklog
                        </Button>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
}

// Sub-component for Labor Entry to handle nested dynamic fields cleanly
function LaborEntryForm({ index, form, remove }: { index: number, form: any, remove: () => void }) {
    const { fields: workerFields, append: appendWorker, remove: removeWorker } = useFieldArray({
        control: form.control,
        name: `labor.${index}.workers`,
    });

    return (
        <div className="border p-4 rounded-md space-y-4 bg-muted/20">
            <div className="flex justify-between items-start">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 flex-1 mr-4">
                    <FormField
                        control={form.control}
                        name={`labor.${index}.contractorName`}
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Contractor Name</FormLabel>
                                <FormControl>
                                    <Input {...field} placeholder="e.g. ABC Construction" />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />
                    <FormField
                        control={form.control}
                        name={`labor.${index}.category`}
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Category</FormLabel>
                                <FormControl>
                                    <Input {...field} placeholder="e.g. Masonry" />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />
                    <FormField
                        control={form.control}
                        name={`labor.${index}.paymentStatus`}
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Payment Status</FormLabel>
                                <Select onValueChange={field.onChange} defaultValue={field.value}>
                                    <FormControl>
                                        <SelectTrigger>
                                            <SelectValue placeholder="Select status" />
                                        </SelectTrigger>
                                    </FormControl>
                                    <SelectContent>
                                        <SelectItem value="Paid">Paid</SelectItem>
                                        <SelectItem value="On Payday">On Payday</SelectItem>
                                        <SelectItem value="Pending">Pending</SelectItem>
                                    </SelectContent>
                                </Select>
                                <FormMessage />
                            </FormItem>
                        )}
                    />
                </div>
                <Button type="button" variant="ghost" size="icon" onClick={remove}>
                    <Trash2 className="h-4 w-4 text-destructive" />
                </Button>
            </div>

            <FormField
                control={form.control}
                name={`labor.${index}.workDescription`}
                render={({ field }) => (
                    <FormItem>
                        <FormLabel>Work Description</FormLabel>
                        <FormControl>
                            <Textarea {...field} placeholder="Describe the work done..." className="h-20" />
                        </FormControl>
                        <FormMessage />
                    </FormItem>
                )}
            />

            <div className="space-y-2">
                <FormLabel>Workers</FormLabel>
                {workerFields.map((workerField, workerIndex) => (
                    <div key={workerField.id} className="flex gap-2 items-center">
                        <FormField
                            control={form.control}
                            name={`labor.${index}.workers.${workerIndex}.workerType`}
                            render={({ field }) => (
                                <FormItem className="flex-1">
                                    <FormControl>
                                        <Input {...field} placeholder="Worker Type (e.g. Mason)" />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name={`labor.${index}.workers.${workerIndex}.count`}
                            render={({ field }) => (
                                <FormItem className="w-24">
                                    <FormControl>
                                        <Input type="number" {...field} placeholder="Count" />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        {workerFields.length > 1 && (
                            <Button type="button" variant="ghost" size="icon" onClick={() => removeWorker(workerIndex)}>
                                <Trash2 className="h-4 w-4 text-destructive" />
                            </Button>
                        )}
                    </div>
                ))}
                <Button
                    type="button"
                    variant="link"
                    size="sm"
                    className="px-0"
                    onClick={() => appendWorker({ workerType: "", count: 0 })}
                >
                    <Plus className="mr-2 h-3 w-3" />
                    Add Worker Type
                </Button>
            </div>
        </div>
    );
}
