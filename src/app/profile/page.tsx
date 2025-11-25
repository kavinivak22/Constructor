'use client';

import { useState, useEffect } from 'react';
import { useSupabase } from '@/supabase/provider';
import { useToast } from '@/hooks/use-toast';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Mail, Building, Calendar, Shield, Briefcase, AlertCircle } from 'lucide-react';
import { format } from 'date-fns';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';

import AvatarUpload from './avatar-upload';
import EditFieldDialog from './edit-field-dialog';
import ChangePasswordDialog from './change-password-dialog';

export default function ProfilePage() {
    const { toast } = useToast();
    const { supabase, user } = useSupabase();
    const [profile, setProfile] = useState<any>(null);
    const [projects, setProjects] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchProfile = async () => {
        if (!user) return;
        setError(null);

        try {
            // Fetch user profile
            const { data: profileData, error: profileError } = await supabase
                .from('users')
                .select(`
                    *,
                    companies (
                        id,
                        name
                    )
                `)
                .eq('id', user.id)
                .single();

            if (profileError) throw profileError;
            setProfile(profileData);

            // Fetch assigned projects
            // Fetch user's projectIds first
            const { data: userData, error: userError } = await supabase
                .from('users')
                .select('projectIds')
                .eq('id', user.id)
                .single();

            if (userError) throw userError;

            const projectIds = userData?.projectIds || [];

            if (projectIds.length > 0) {
                const { data: projectsData, error: projectsError } = await supabase
                    .from('projects')
                    .select('id, name, status, clientName, endDate')
                    .in('id', projectIds);

                if (projectsError) throw projectsError;
                setProjects(projectsData || []);
            } else {
                setProjects([]);
            }

        } catch (error: any) {
            console.error('Error fetching profile data:', error);
            setError('Failed to load profile data. Please try again later.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchProfile();
    }, [supabase, user]);

    if (loading) {
        return (
            <div className="container mx-auto p-6 space-y-6 max-w-5xl">
                <Skeleton className="h-48 w-full rounded-xl" />
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <Skeleton className="h-64 md:col-span-1 rounded-xl" />
                    <Skeleton className="h-64 md:col-span-2 rounded-xl" />
                </div>
            </div>
        )
    }

    if (!profile && !error) {
        return (
            <div className="flex flex-col h-full items-center justify-center">
                <p className="text-muted-foreground">Profile not found.</p>
            </div>
        )
    }

    return (
        <div className="flex flex-col min-h-screen bg-muted/10">
            {/* Header / Cover */}
            <div className="h-48 bg-gradient-to-r from-blue-600 to-indigo-700 w-full relative">
                <div className="absolute -bottom-16 left-6 md:left-10">
                    {profile && (
                        <AvatarUpload
                            url={profile.photoURL}
                            onUpload={(url) => setProfile({ ...profile, photoURL: url })}
                        />
                    )}
                </div>
            </div>

            <main className="container mx-auto p-6 pt-20 max-w-5xl space-y-8">
                {error && (
                    <Alert variant="destructive">
                        <AlertCircle className="h-4 w-4" />
                        <AlertTitle>Error</AlertTitle>
                        <AlertDescription>
                            {error}
                        </AlertDescription>
                    </Alert>
                )}

                {profile && (
                    <>

                        {/* Header Info */}
                        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                            <div>
                                <h1 className="text-3xl font-bold tracking-tight flex items-center gap-3">
                                    {profile.displayName || 'User'}
                                    <EditFieldDialog
                                        field="displayName"
                                        label="Display Name"
                                        currentValue={profile.displayName || ''}
                                        role={profile.role}
                                        companyId={profile.companyId}
                                        onUpdate={fetchProfile}
                                    />
                                </h1>
                                <div className="flex items-center gap-2 text-muted-foreground mt-1">
                                    <span className="capitalize font-medium text-foreground">{profile.role}</span>
                                    <span>•</span>
                                    <span className="flex items-center gap-1">
                                        <Building className="h-3 w-3" />
                                        {profile.companies?.name || 'No Company'}
                                    </span>
                                </div>
                            </div>
                            <div className="flex items-center gap-2">
                                {/* Actions like "View Public Profile" could go here */}
                            </div>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">

                            {/* Left Column: Personal Info */}
                            <div className="md:col-span-1 space-y-6">
                                <Card>
                                    <CardHeader>
                                        <CardTitle className="text-lg">Contact Information</CardTitle>
                                    </CardHeader>
                                    <CardContent className="space-y-4">
                                        <div className="space-y-1">
                                            <label className="text-xs font-medium text-muted-foreground uppercase">Email</label>
                                            <div className="flex items-center gap-2 text-sm">
                                                <Mail className="h-4 w-4 text-muted-foreground" />
                                                {profile.email}
                                            </div>
                                        </div>
                                        <div className="space-y-1">
                                            <div className="flex items-center justify-between">
                                                <label className="text-xs font-medium text-muted-foreground uppercase">Phone</label>
                                                <EditFieldDialog
                                                    field="phone"
                                                    label="Phone Number"
                                                    currentValue={profile.phone || ''}
                                                    role={profile.role}
                                                    companyId={profile.companyId}
                                                    onUpdate={fetchProfile}
                                                />
                                            </div>
                                            <div className="text-sm font-medium">
                                                {profile.phone || 'Not provided'}
                                            </div>
                                        </div>
                                        <div className="space-y-1">
                                            <label className="text-xs font-medium text-muted-foreground uppercase">Joined</label>
                                            <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                                <Calendar className="h-4 w-4" />
                                                {profile.created_at ? format(new Date(profile.created_at), 'MMMM d, yyyy') : 'Unknown'}
                                            </div>
                                        </div>
                                    </CardContent>
                                </Card>
                            </div>

                            {/* Right Column: Account Settings & Projects */}
                            <div className="md:col-span-2 space-y-6">

                                {/* Assigned Projects */}
                                <Card>
                                    <CardHeader>
                                        <CardTitle className="text-lg flex items-center gap-2">
                                            <Briefcase className="h-5 w-5 text-primary" />
                                            Projects
                                        </CardTitle>
                                        <CardDescription>
                                            Projects you are currently working on.
                                        </CardDescription>
                                    </CardHeader>
                                    <CardContent>
                                        {projects.length === 0 ? (
                                            <p className="text-sm text-muted-foreground">No projects assigned yet.</p>
                                        ) : (
                                            <div className="grid gap-4">
                                                {projects.map((project) => (
                                                    <div key={project.id} className="flex items-center justify-between p-4 border rounded-lg bg-background hover:bg-muted/50 transition-colors">
                                                        <div className="space-y-1">
                                                            <div className="flex items-center gap-2">
                                                                <p className="font-medium">{project.name}</p>
                                                                <Badge variant="outline" className="capitalize text-xs">
                                                                    {project.status}
                                                                </Badge>
                                                            </div>
                                                            <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                                                <span>{project.clientName || 'Internal Project'}</span>
                                                                <span>•</span>
                                                                <span className="capitalize font-medium text-primary/80">
                                                                    {profile.role} {/* Displaying global role as per requirement */}
                                                                </span>
                                                            </div>
                                                        </div>
                                                        {project.endDate && (
                                                            <div className="text-xs text-muted-foreground text-right">
                                                                <p>Due</p>
                                                                <p>{format(new Date(project.endDate), 'MMM d, yyyy')}</p>
                                                            </div>
                                                        )}
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </CardContent>
                                </Card>

                                <Card>
                                    <CardHeader>
                                        <CardTitle className="text-lg flex items-center gap-2">
                                            <Shield className="h-5 w-5 text-primary" />
                                            Security & Login
                                        </CardTitle>
                                        <CardDescription>
                                            Manage your password and account security preferences.
                                        </CardDescription>
                                    </CardHeader>
                                    <CardContent className="space-y-6">
                                        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between p-4 border rounded-lg bg-background gap-4">
                                            <div className="space-y-1">
                                                <p className="font-medium">Password</p>
                                                <p className="text-sm text-muted-foreground">
                                                    Last changed never (or unknown)
                                                </p>
                                            </div>
                                            <ChangePasswordDialog />
                                        </div>
                                        {/* Add 2FA or other settings here later */}
                                    </CardContent>
                                </Card>

                                {/* Activity or other sections could go here */}
                            </div>
                        </div>
                    </>
                )}
            </main>
        </div>
    );
}
