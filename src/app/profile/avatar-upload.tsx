'use client';

import { useState } from 'react';
import { useSupabase } from '@/supabase/provider';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Loader2, Upload, User as UserIcon } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';

interface AvatarUploadProps {
    url: string | null;
    onUpload: (url: string) => void;
    editable?: boolean;
}

export default function AvatarUpload({ url, onUpload, editable = true }: AvatarUploadProps) {
    const { supabase, user } = useSupabase();
    const { toast } = useToast();
    const [uploading, setUploading] = useState(false);

    const uploadAvatar = async (event: React.ChangeEvent<HTMLInputElement>) => {
        try {
            setUploading(true);

            if (!event.target.files || event.target.files.length === 0) {
                throw new Error('You must select an image to upload.');
            }

            const file = event.target.files[0];
            const fileExt = file.name.split('.').pop();
            const filePath = `${user?.id}-${Math.random()}.${fileExt}`;

            const { error: uploadError } = await supabase.storage
                .from('avatars')
                .upload(filePath, file);

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
                .eq('id', user?.id);

            if (updateError) {
                throw updateError;
            }

            onUpload(publicUrl);
            toast({
                title: 'Success',
                description: 'Avatar updated successfully.',
            });

        } catch (error: any) {
            toast({
                title: 'Error',
                description: error.message,
                variant: 'destructive',
            });
        } finally {
            setUploading(false);
        }
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
                        onChange={uploadAvatar}
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
        </div>
    );
}
