package com.example.data.db

import kotlinx.coroutines.flow.Flow

class NotesRepository(
    private val noteDao: NoteDao,
) {
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    val allFolders: Flow<List<NoteFolderEntity>> = noteDao.getAllFolders()

    fun searchNotes(category: String, query: String): Flow<List<NoteEntity>> =
        noteDao.searchNotes(category, query)

    suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)

    suspend fun insertNote(note: NoteEntity): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: NoteEntity) = noteDao.updateNote(note)

    suspend fun deleteNote(id: Long) = noteDao.deleteNoteById(id)

    suspend fun togglePin(id: Long, currentlyPinned: Boolean) {
        noteDao.setPinned(id, !currentlyPinned)
    }

    suspend fun createFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            noteDao.insertFolder(NoteFolderEntity(name = trimmed))
        }
    }

    suspend fun deleteFolder(name: String) {
        if (name != "Personal Notes") {
            noteDao.moveNotesToPersonal(name)
            noteDao.deleteFolderByName(name)
        }
    }

    suspend fun seedDefaultsIfEmpty() {
        if (noteDao.countFolders() == 0) {
            noteDao.insertFolders(
                listOf(
                    NoteFolderEntity(name = "Python Basics"),
                    NoteFolderEntity(name = "Algorithms"),
                    NoteFolderEntity(name = "RoboContest"),
                    NoteFolderEntity(name = "Important Codes"),
                    NoteFolderEntity(name = "Python Libraries"),
                    NoteFolderEntity(name = "Personal Notes"),
                )
            )
        }
        if (noteDao.countNotes() == 0) {
            val now = System.currentTimeMillis()
            noteDao.insertNotes(
                listOf(
                    NoteEntity(
                        title = "Functions, Classes & Recursion",
                        description = "Clean Python 3 syntax for classes, type hints, and memoized recursion.",
                        codeSnippet = """from functools import lru_cache

@lru_cache(maxsize=None)
def fibonacci(n: int) -> int:
    if n <= 1:
        return n
    return fibonacci(n - 1) + fibonacci(n - 2)

class Counter:
    def __init__(self, start: int = 0):
        self.value = start

    def inc(self, step: int = 1) -> int:
        self.value += step
        return self.value

print("Fib(12):", fibonacci(12))
c = Counter(10)
print("Counter:", c.inc(5))""",
                        category = "Python Basics",
                        isPinned = true,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    NoteEntity(
                        title = "Binary Search & BFS Template",
                        description = "Standard O(log N) binary search and deque-based breadth-first search.",
                        codeSnippet = """from collections import deque

def binary_search(arr, target):
    lo, hi = 0, len(arr) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if arr[mid] == target:
            return mid
        elif arr[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1

def bfs(graph, start):
    visited = {start}
    queue = deque([start])
    order = []
    while queue:
        node = queue.popleft()
        order.append(node)
        for nxt in graph.get(node, []):
            if nxt not in visited:
                visited.add(nxt)
                queue.append(nxt)
    return order

print("Index:", binary_search([2, 5, 8, 12, 16, 23], 12))""",
                        category = "Algorithms",
                        isPinned = true,
                        createdAt = now - 1000,
                        updatedAt = now - 1000,
                    ),
                    NoteEntity(
                        title = "RoboContest Fast Input & Modular Math",
                        description = "Competitive programming starter with fast token parsing and pow(a, b, mod).",
                        codeSnippet = """import sys

MOD = 10**9 + 7

def solve():
    # Use input() for interactive tests
    n = int(input("Enter N: "))
    total = sum(range(1, n + 1)) % MOD
    print(f"Sum 1..{n} mod 10^9+7 =", total)

if __name__ == "__main__":
    solve()""",
                        category = "RoboContest",
                        isPinned = false,
                        createdAt = now - 2000,
                        updatedAt = now - 2000,
                    ),
                    NoteEntity(
                        title = "JSON & Regex Quick Reference",
                        description = "Parse JSON strings safely and extract patterns with re.findall.",
                        codeSnippet = """import json
import re

payload = '{"user": "ada", "scores": [98, 95, 100]}'
data = json.loads(payload)
print("Average:", sum(data["scores"]) / len(data["scores"]))

text = "Orders: #1042, #2089, #3110"
ids = re.findall(r"#(\d+)", text)
print("Extracted IDs:", ids)""",
                        category = "Important Codes",
                        isPinned = false,
                        createdAt = now - 3000,
                        updatedAt = now - 3000,
                    ),
                    NoteEntity(
                        title = "NumPy, Pandas & Matplotlib Plot",
                        description = "Create arrays, analyze tabular data, and render an inline Matplotlib chart.",
                        codeSnippet = """import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

x = np.linspace(0, 2 * np.pi, 100)
df = pd.DataFrame({"x": x, "sin": np.sin(x), "cos": np.cos(x)})
print(df.head())

plt.figure(figsize=(6, 3.5))
plt.plot(df["x"], df["sin"], label="sin(x)", color="#38BDF8")
plt.plot(df["x"], df["cos"], label="cos(x)", color="#FBBF24")
plt.title("Trigonometric Waves", color="white")
plt.legend()
plt.grid(alpha=0.2)
plt.show()""",
                        category = "Python Libraries",
                        isPinned = false,
                        createdAt = now - 4000,
                        updatedAt = now - 4000,
                    ),
                    NoteEntity(
                        title = "Personal Scratchpad",
                        description = "Quick ideas, reminders, and test snippets.",
                        codeSnippet = """# Write your personal Python notes here
items = {"python": 3.11, "offline": True}
for k, v in items.items():
    print(f"{k} -> {v}")""",
                        category = "Personal Notes",
                        isPinned = false,
                        createdAt = now - 5000,
                        updatedAt = now - 5000,
                    ),
                )
            )
        }
    }
}
