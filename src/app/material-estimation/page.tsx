import { EstimationForm } from "@/components/material-estimation/estimation-form";

export default function MaterialEstimationPage() {
  return (
    <div className="flex flex-col h-full">
       <header className="flex items-center gap-2 p-4 border-b md:px-6 shrink-0 bg-background sticky top-0 z-10">
        <div className="flex items-center gap-2">
          <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline">
            AI-Assisted Material Estimation
          </h1>
        </div>
      </header>
      <main className="flex-1 p-4 overflow-y-auto md:p-6">
        <div className="max-w-2xl mx-auto">
            <p className="mb-6 text-muted-foreground">
                Use our AI assistant to generate material estimations based on your project specifications.
                Provide the details below to get started.
            </p>
            <EstimationForm />
        </div>
      </main>
    </div>
  );
}
