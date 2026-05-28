'use server';

import { createClient } from '@/utils/supabase/server';
import { revalidatePath } from 'next/cache';

export interface PersonalNote {
  id: string;
  user_id: string;
  title: string;
  description: string | null;
  priority: 'High' | 'Medium' | 'Low';
  location: string | null;
  due_date: string | null;
  is_completed: boolean;
  created_at: string;
}

export interface PersonalDocument {
  id: string;
  user_id: string;
  name: string;
  url: string;
  size: number;
  type: string;
  category: string;
  created_at: string;
}

// Fetch all personal notes for the current user
export async function getPersonalNotes(): Promise<PersonalNote[]> {
  const supabase = await createClient();

  const { data: { user } } = await supabase.auth.getUser();
  if (!user) throw new Error('Not authenticated');

  const { data, error } = await supabase
    .from('personal_notes')
    .select('*')
    .eq('user_id', user.id)
    .order('created_at', { ascending: false });

  if (error) {
    console.error('Error fetching personal notes:', error);
    throw error;
  }

  return data || [];
}

// Create a new personal note
export async function createPersonalNote(input: {
  title: string;
  description?: string;
  priority: 'High' | 'Medium' | 'Low';
  location?: string;
  due_date?: string | null;
}) {
  const supabase = await createClient();

  const { data: { user } } = await supabase.auth.getUser();
  if (!user) throw new Error('Not authenticated');

  const { data, error } = await supabase
    .from('personal_notes')
    .insert([
      {
        user_id: user.id,
        title: input.title,
        description: input.description || null,
        priority: input.priority,
        location: input.location || null,
        due_date: input.due_date || null,
        is_completed: false
      }
    ])
    .select()
    .single();

  if (error) {
    console.error('Error creating personal note:', error);
    throw error;
  }

  revalidatePath('/personal-pouch');
  return data;
}

// Update a personal note
export async function updatePersonalNote(
  id: string,
  updates: {
    title?: string;
    description?: string | null;
    priority?: 'High' | 'Medium' | 'Low';
    location?: string | null;
    due_date?: string | null;
    is_completed?: boolean;
  }
) {
  const supabase = await createClient();

  const { data: { user } } = await supabase.auth.getUser();
  if (!user) throw new Error('Not authenticated');

  const { data, error } = await supabase
    .from('personal_notes')
    .update(updates)
    .eq('id', id)
    .eq('user_id', user.id) // Security check
    .select()
    .single();

  if (error) {
    console.error('Error updating personal note:', error);
    throw error;
  }

  revalidatePath('/personal-pouch');
  return data;
}

// Delete a personal note
export async function deletePersonalNote(id: string) {
  const supabase = await createClient();

  const { data: { user } } = await supabase.auth.getUser();
  if (!user) throw new Error('Not authenticated');

  const { error } = await supabase
    .from('personal_notes')
    .delete()
    .eq('id', id)
    .eq('user_id', user.id); // Security check

  if (error) {
    console.error('Error deleting personal note:', error);
    throw error;
  }

  revalidatePath('/personal-pouch');
  return { success: true };
}

// Fetch all personal documents for the current user
export async function getPersonalDocuments(): Promise<PersonalDocument[]> {
  const supabase = await createClient();

  const { data: { user } } = await supabase.auth.getUser();
  if (!user) throw new Error('Not authenticated');

  const { data, error } = await supabase
    .from('personal_documents')
    .select('*')
    .eq('user_id', user.id)
    .order('created_at', { ascending: false });

  if (error) {
    console.error('Error fetching personal documents:', error);
    throw error;
  }

  return data || [];
}

// Save personal document metadata
export async function savePersonalDocumentMetadata(input: {
  name: string;
  url: string;
  size: number;
  type: string;
  category: string;
}) {
  const supabase = await createClient();

  const { data: { user } } = await supabase.auth.getUser();
  if (!user) throw new Error('Not authenticated');

  const { data, error } = await supabase
    .from('personal_documents')
    .insert([
      {
        user_id: user.id,
        name: input.name,
        url: input.url,
        size: input.size,
        type: input.type,
        category: input.category
      }
    ])
    .select()
    .single();

  if (error) {
    console.error('Error saving personal document metadata:', error);
    throw error;
  }

  revalidatePath('/personal-pouch');
  return data;
}

// Delete a personal document from DB and Storage
export async function deletePersonalDocument(id: string) {
  const supabase = await createClient();

  const { data: { user } } = await supabase.auth.getUser();
  if (!user) throw new Error('Not authenticated');

  // 1. Fetch metadata first to get the URL
  const { data: doc, error: fetchError } = await supabase
    .from('personal_documents')
    .select('*')
    .eq('id', id)
    .eq('user_id', user.id)
    .single();

  if (fetchError || !doc) {
    throw new Error('Document not found or access denied');
  }

  // 2. Delete from database
  const { error: dbError } = await supabase
    .from('personal_documents')
    .delete()
    .eq('id', id)
    .eq('user_id', user.id);

  if (dbError) throw dbError;

  // 3. Extract path and delete from storage
  const urlParts = doc.url.split('/personal-documents/');
  const storagePath = urlParts[1];
  if (storagePath) {
    const { error: storageError } = await supabase.storage
      .from('personal-documents')
      .remove([decodeURIComponent(storagePath)]);
    
    if (storageError) {
      console.warn('Could not delete file from storage bucket:', storageError);
    }
  }

  revalidatePath('/personal-pouch');
  return { success: true };
}
