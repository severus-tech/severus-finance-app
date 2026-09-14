import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { TodoItem } from '@/types';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Dialog } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/common/EmptyState';
import { formatDate } from '@/lib/utils';
import { CheckSquare, Plus, Circle, CheckCircle2, Trash2, Calendar } from 'lucide-react';
import { toast } from 'sonner';

export const TodoPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [filterStatus, setFilterStatus] = useState<string>('PENDING');
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  // Form State
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [priority, setPriority] = useState<'LOW' | 'MEDIUM' | 'HIGH'>('MEDIUM');

  const { data: todos, isLoading } = useQuery<TodoItem[]>({
    queryKey: ['todos', filterStatus],
    queryFn: async () => {
      const param = filterStatus !== 'ALL' ? `?status=${filterStatus}` : '';
      const res = await api.get(`/api/v1/todos${param}`);
      return res.data;
    },
  });

  const handleCreateTodo = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      await api.post('/api/v1/todos', {
        title,
        description,
        dueDate: dueDate || null,
        priority,
      });

      toast.success('Task created!');
      setIsAddOpen(false);
      setTitle('');
      setDescription('');
      queryClient.invalidateQueries({ queryKey: ['todos'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    } catch {
      toast.error('Failed to create task');
    } finally {
      setLoading(false);
    }
  };

  const handleToggle = async (id: string) => {
    try {
      await api.patch(`/api/v1/todos/${id}/toggle`);
      toast.success('Task status updated');
      queryClient.invalidateQueries({ queryKey: ['todos'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    } catch {
      toast.error('Failed to update task');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await api.delete(`/api/v1/todos/${id}`);
      toast.success('Task deleted');
      queryClient.invalidateQueries({ queryKey: ['todos'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    } catch {
      toast.error('Failed to delete task');
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-100 font-heading tracking-tight">
            Financial Reminders & Tasks
          </h1>
          <p className="text-xs text-slate-400">
            Never miss bill due dates, policy renewals, or financial tasks
          </p>
        </div>

        <Button variant="primary" onClick={() => setIsAddOpen(true)}>
          <Plus className="w-4 h-4" />
          Add Task
        </Button>
      </div>

      {/* Filters */}
      <div className="flex items-center gap-2 border-b border-purple-900/20 pb-3">
        {['PENDING', 'COMPLETED', 'ALL'].map((st) => (
          <button
            key={st}
            onClick={() => setFilterStatus(st)}
            className={`px-4 py-2 text-xs font-semibold rounded-xl transition-all ${
              filterStatus === st
                ? 'bg-purple-600 text-white shadow-md'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
            }`}
          >
            {st === 'ALL' ? 'All Tasks' : st.charAt(0) + st.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {/* Todo List */}
      {isLoading ? (
        <Skeleton className="h-64 w-full" />
      ) : todos && todos.length > 0 ? (
        <div className="space-y-3">
          {todos.map((todo) => (
            <Card
              key={todo.id}
              className={`p-4 transition-all duration-200 flex items-center justify-between ${
                todo.status === 'COMPLETED' ? 'opacity-60 bg-slate-900/40' : ''
              }`}
            >
              <div className="flex items-center gap-4 min-w-0">
                <button
                  onClick={() => handleToggle(todo.id)}
                  className="text-slate-400 hover:text-purple-400 transition-colors"
                >
                  {todo.status === 'COMPLETED' ? (
                    <CheckCircle2 className="w-6 h-6 text-emerald-400" />
                  ) : (
                    <Circle className="w-6 h-6" />
                  )}
                </button>
                <div className="min-w-0">
                  <p
                    className={`text-sm font-bold text-slate-100 ${
                      todo.status === 'COMPLETED' ? 'line-through text-slate-400' : ''
                    }`}
                  >
                    {todo.title}
                  </p>
                  {todo.description && <p className="text-xs text-slate-400 mt-0.5">{todo.description}</p>}
                  {todo.dueDate && (
                    <p className="text-[11px] text-purple-400 flex items-center gap-1 mt-1 font-semibold">
                      <Calendar className="w-3 h-3" /> Due {formatDate(todo.dueDate)}
                    </p>
                  )}
                </div>
              </div>

              <div className="flex items-center gap-3">
                <Badge variant={todo.priority === 'HIGH' ? 'rose' : todo.priority === 'MEDIUM' ? 'amber' : 'slate'}>
                  {todo.priority}
                </Badge>
                <button
                  onClick={() => handleDelete(todo.id)}
                  className="p-2 text-slate-400 hover:text-rose-400 transition-colors"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </Card>
          ))}
        </div>
      ) : (
        <EmptyState
          icon={CheckSquare}
          title="No tasks found"
          description="Create to-do items and bill reminders to keep your finances organized."
          actionText="Add Task"
          onAction={() => setIsAddOpen(true)}
        />
      )}

      {/* Add Task Dialog */}
      <Dialog isOpen={isAddOpen} onClose={() => setIsAddOpen(false)} title="Create Task">
        <form onSubmit={handleCreateTodo} className="space-y-4">
          <Input label="Title" value={title} onChange={(e) => setTitle(e.target.value)} required placeholder="e.g. Pay Health Insurance Premium" />
          <Input label="Description (Optional)" value={description} onChange={(e) => setDescription(e.target.value)} placeholder="e.g. Policy #12345, Star Health" />
          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Priority"
              value={priority}
              onChange={(e) => setPriority(e.target.value as any)}
              options={[
                { value: 'LOW', label: 'Low Priority' },
                { value: 'MEDIUM', label: 'Medium Priority' },
                { value: 'HIGH', label: 'High Priority' },
              ]}
            />
            <Input label="Due Date (Optional)" type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-purple-900/20">
            <Button variant="ghost" type="button" onClick={() => setIsAddOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" isLoading={loading}>
              Save Task
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
};
