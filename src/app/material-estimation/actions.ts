"use server";
import {
  getMaterialEstimationStream,
  type MaterialEstimationInput
} from "@/ai/flows/material-estimation-from-project-specs";
import { parseEstimationText, type ParsedMaterial } from "@/ai/flows/parse-estimation";
import { createClient } from "@/utils/supabase/server";
import { z } from "zod";

const FormSchema = z.object({
  projectType: z.string().min(1, "Project type is required."),
  projectSize: z.string().min(1, "Project size is required."),
  projectLocation: z.string().min(1, "Project location is required."),
  specificRequirements: z.string().min(1, "Specific requirements are required."),
});

export type FormState = {
  message: string;
  estimation?: string; // Plain string — the complete AI-generated text
  fields?: Record<string, string>;
  issues?: string[];
};

export async function getMaterialEstimation(
  prevState: FormState,
  formData: FormData
): Promise<FormState> {
  const validatedFields = FormSchema.safeParse(Object.fromEntries(formData.entries()));

  if (!validatedFields.success) {
    return {
      message: "Please fill out all required fields.",
      fields: {
        projectType: String(formData.get("projectType") || ""),
        projectSize: String(formData.get("projectSize") || ""),
        projectLocation: String(formData.get("projectLocation") || ""),
        specificRequirements: String(formData.get("specificRequirements") || ""),
      },
      issues: validatedFields.error.issues.map((issue) => issue.message),
    };
  }

  const input: MaterialEstimationInput = validatedFields.data;

  try {
    // Collect the full streamed output into a string
    let fullEstimation = '';
    await getMaterialEstimationStream(input, (chunk) => {
      fullEstimation += chunk;
    });

    return {
      message: "Estimation successful.",
      estimation: fullEstimation,
    };
  } catch (error: any) {
    console.error("AI estimation error:", error);
    return {
      message: "Failed to generate estimation. Please try again.",
      issues: [error?.message || "Unknown AI error"],
    };
  }
}

// Fetch assigned projects for the dropdown selector
export async function getAssignedProjects() {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) throw new Error('Not authenticated');

  const { data: userData, error: userError } = await supabase
    .from('users')
    .select('companyId')
    .eq('id', user.id)
    .single();

  if (userError || !userData?.companyId) return [];

  const { data: projects, error: projectsError } = await supabase
    .from('projects')
    .select('id, name')
    .eq('companyId', userData.companyId);

  if (projectsError) {
    console.error("Error fetching projects:", projectsError);
    return [];
  }

  return projects || [];
}

// Parse raw estimation report into structured material entities
export async function parseEstimationReport(rawText: string): Promise<ParsedMaterial[]> {
  try {
    return await parseEstimationText(rawText);
  } catch (error) {
    console.error("Error calling parsing flow:", error);
    throw new Error("Failed to parse estimation content.");
  }
}

// Save parsed material items into public.materials
export async function importProjectMaterials(projectId: string, materials: ParsedMaterial[]) {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) throw new Error('Not authenticated');

  const { data: project, error: projectError } = await supabase
    .from('projects')
    .select('site_id')
    .eq('id', projectId)
    .single();

  if (projectError || !project || !project.site_id) {
    throw new Error('Project or associated Site not found');
  }

  const insertData = materials.map(m => ({
    site_id: project.site_id,
    name: m.name,
    category: m.category,
    current_stock: m.quantity || 0,
    unit_of_measurement: m.unit || '',
    unit_cost: m.cost || 0
  }));

  const { error } = await supabase
    .from('materials')
    .insert(insertData);

  if (error) {
    console.error("Error importing materials:", error);
    throw error;
  }

  return { success: true };
}

