'use client';

import { useState } from 'react';
import { useSupabase } from '@/supabase/provider';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Loader2, Upload, User as UserIcon } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import ImageCropperDialog from './image-cropper-dialog';

interface AvatarUploadProps {
    url: string | null;
    onUpload: (url: string) => void;
    editable?: boolean;
}

export default function AvatarUpload({ url, onUpload, editable = true }: AvatarUploadProps) {
    const { supabase, user } = useSupabase();
    const { toast } = useToast();
    const [uploading, setUploading] = useState(false);
    const [cropperOpen, setCropperOpen] = useState(false);
    const [selectedImage, setSelectedImage] = useState<string | null>(null);

    const onFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (event.target.files && event.target.files.length > 0) {
            const file = event.target.files[0];
            const reader = new FileReader();
            reader.addEventListener('load', () => {
                setSelectedImage(reader.result as string);
                setCropperOpen(true);
            });
            reader.readAsDataURL(file);
            // Reset input value so the same file can be selected again
            event.target.value = '';
        }
    };

    const handleCropComplete = async (croppedImageBlob: Blob) => {
        setCropperOpen(false);
        setUploading(true);

        try {
            if (!user) throw new Error('User not found');

            // 1. List existing files in the user's folder (to delete later)
            const { data: existingFiles } = await supabase.storage
                .from('avatars')
                .list(user.id);

            const fileExt = 'jpg'; // Cropped image is always jpeg
            const filePath = `${user.id}/${Math.random()}.${fileExt}`;

            const { error: uploadError } = await supabase.storage
                .from('avatars')
                .upload(filePath, croppedImageBlob, {
                    contentType: 'image/jpeg',
                    upsert: true
                });

            if (uploadError) {
                throw uploadError;
            }

            const { data: { publicUrl } } = supabase.storage
                .from('avatars')
                .getPublicUrl(filePath);

            // Update user profile
            const { error: updateError } = await supabase
                .from('users')
                .update({ "photoURL": publicUrl })
                .eq('id', user.id);

            if (updateError) {
                throw updateError;
            }

            // 2. Delete old files ONLY after successful upload and profile update
            if (existingFiles && existingFiles.length > 0) {
                const filesToRemove = existingFiles.map(x => `${user.id}/${x.name}`);
                // Ensure we don't delete the file we just uploaded (just in case)
                const filesToDelete = filesToRemove.filter(path => path !== filePath);

                if (filesToDelete.length > 0) {
                    await supabase.storage
                        .from('avatars')
                        .remove(filesToDelete);
                }
            }

            onUpload(publicUrl);
            toast({
                title: 'Success',
                description: 'Avatar updated successfully.',
            });

        } catch (error: any) {
            console.error(error);
            toast({
                title: 'Error',
                description: error.message || 'Failed to upload avatar.',
                variant: 'destructive',
            });
        } finally {
            setUploading(false);
            setSelectedImage(null);
        }
    };

    const handleCancel = () => {
        setCropperOpen(false);
        setSelectedImage(null);
    };

    return (
        <div className="relative group">
            <Avatar className="w-32 h-32 border-4 border-background shadow-xl">
                <AvatarImage src={url || undefined} alt="Avatar" className="object-cover" />
                <AvatarFallback className="text-4xl bg-muted">
                    <UserIcon className="h-16 w-16 text-muted-foreground" />
                </AvatarFallback>
            </Avatar>

            {editable && (
                <div className="absolute bottom-0 right-0">
                    <input
                        type="file"
                        id="avatar-upload"
                        accept="image/*"
                        onChange={onFileSelect}
                        disabled={uploading}
                        className="hidden"
                    />
                    <label htmlFor="avatar-upload">
                        <Button
                            size="icon"
                            variant="secondary"
                            className="rounded-full h-10 w-10 shadow-md cursor-pointer hover:scale-105 transition-transform"
                            asChild
                            disabled={uploading}
                        >
                            <span>
                                {uploading ? (
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                ) : (
                                    <Upload className="h-4 w-4" />
                                )}
                            </span>
                        </Button>
                    </label>
                </div>
            )}

            {selectedImage && (
                <ImageCropperDialog
                    open={cropperOpen}
                    onOpenChange={setCropperOpen}
                    imageSrc={selectedImage}
                    onCropComplete={handleCropComplete}
                    onCancel={handleCancel}
                />
            )}
        </div>
    );
}
