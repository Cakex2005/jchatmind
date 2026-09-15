import { useCallback, useEffect, useState } from "react";
import {
  type AgentVO,
  createAgent,
  type CreateAgentRequest,
  getAgents,
  deleteAgent,
  updateAgent,
  type UpdateAgentRequest,
} from "../api/api.ts";

export function useAgents() {
  const [agents, setAgents] = useState<AgentVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refreshAgents = useCallback(async () => {
    setLoading(true);
    try {
      const resp = await getAgents();
      setAgents(resp.agents ?? []);
      setError(null);
    } catch (err) {
      // 加载失败必须让调用方可见, 否则页面会停在空列表上却没有任何提示
      setAgents([]);
      setError(err instanceof Error ? err.message : "加载智能体列表失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshAgents().then();
  }, [refreshAgents]);

  async function createAgentHandle(agent: CreateAgentRequest) {
    await createAgent(agent);
    await refreshAgents();
  }

  async function deleteAgentHandle(agentId: string) {
    await deleteAgent(agentId);
    await refreshAgents();
  }

  async function updateAgentHandle(
    agentId: string,
    request: UpdateAgentRequest,
  ) {
    await updateAgent(agentId, request);
    await refreshAgents();
  }

  return {
    agents,
    loading,
    error,
    createAgentHandle,
    deleteAgentHandle,
    updateAgentHandle,
    refreshAgents,
  };
}
