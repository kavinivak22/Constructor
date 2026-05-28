'use server';

import { ai } from '@/ai/genkit';
import { z } from 'genkit';

const ParsedMaterialSchema = z.object({
  name: z.string().describe('Name of the material (e.g. Portland Cement, Rebar, Timber)'),
  category: z.string().describe('Category of the material (e.g. Concrete, Steel, Wood, Electrical)'),
  quantity: z.number().describe('Estimated quantity'),
  unit: z.string().describe('Unit of measurement (e.g. bags, tons, pieces, linear feet)'),
  cost: z.number().describe('Estimated unit cost or total cost of the material in INR'),
});

export type ParsedMaterial = z.infer<typeof ParsedMaterialSchema>;

const ParseEstimationInputSchema = z.object({
  rawText: z.string(),
});

const ParseEstimationOutputSchema = z.object({
  materials: z.array(ParsedMaterialSchema),
});

export async function parseEstimationText(rawText: string): Promise<ParsedMaterial[]> {
  const result = await parseEstimationFlow({ rawText });
  return result.materials;
}

export const parseEstimationFlow = ai.defineFlow(
  {
    name: 'parseEstimationFlow',
    inputSchema: ParseEstimationInputSchema,
    outputSchema: ParseEstimationOutputSchema,
  },
  async (input) => {
    const prompt = `You are a material estimating assistant. Your job is to extract a structured list of materials from the following unstructured text estimation.
    
    Unstructured Estimation:
    ${input.rawText}
    
    Extract all materials mentioned, categorize them, determine the quantity, unit of measurement, and estimated cost. Return them in the requested structured format.`;

    const response = await ai.generate({
      prompt,
      output: {
        schema: ParseEstimationOutputSchema
      }
    });

    return response.output!;
  }
);
