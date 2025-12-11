"use server";
import { estimateMaterials, getMaterialEstimationStream, type MaterialEstimationInput, type MaterialEstimationOutput } from "@/ai/flows/material-estimation-from-project-specs";
import { createStreamableValue } from 'ai';
import { z } from "zod";

const FormSchema = z.object({
  projectType: z.string().min(1, "Project type is required."),
  projectSize: z.string().min(1, "Project size is required."),
  projectLocation: z.string().min(1, "Project location is required."),
  specificRequirements: z.string().min(1, "Specific requirements are required."),
});

export type FormState = {
  message: string;
  estimation?: MaterialEstimationOutput;
  fields?: Record<string, string>;
  issues?: string[];
};

export async function getMaterialEstimation(
  prevState: FormState,
  formData: FormData
) {
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
  const stream = createStreamableValue();

  // Can't be awaited
  getMaterialEstimationStream(input, stream);

  return {
    message: "Estimation successful.",
    estimation: { estimation: stream.value },
  };
}
