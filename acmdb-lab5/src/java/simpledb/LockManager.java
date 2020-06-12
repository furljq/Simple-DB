package simpledb;

import java.util.*;
import java.util.concurrent.*;

public class LockManager {
    private final Map<PageId, Object> locks;
    private final Map<PageId, TransactionId> pageExcludedByTransaction;
    private final Map<PageId, Collection<TransactionId>> pageSharedByTransactions;
    private final Map<TransactionId, Collection<PageId>> transactionHoldPages;
    private final Map<TransactionId, Collection<PageId>> transactionHoldDirtyPages;
    private final Map<TransactionId, Collection<TransactionId>> dependencyGraph;

    public LockManager() {
        locks = new ConcurrentHashMap<>();
        pageSharedByTransactions = new ConcurrentHashMap<>();
        pageExcludedByTransaction = new ConcurrentHashMap<>();
        transactionHoldPages = new ConcurrentHashMap<>();
        transactionHoldDirtyPages = new ConcurrentHashMap<>();
        dependencyGraph = new ConcurrentHashMap<>();
    }

    private Object getLock(PageId pid) {
        locks.putIfAbsent(pid, new Object());
        return locks.get(pid);
    }

    public void acquireLock(TransactionId tid, PageId pid, Permissions perm) throws TransactionAbortedException {
        if (perm == Permissions.READ_ONLY) {
            if (tid.equals(pageExcludedByTransaction.get(pid))) return;
            if (pageSharedByTransactions.containsKey(pid) && pageSharedByTransactions.get(pid).contains(tid)) return;
            acquireShareLock(tid, pid);
        } else if (perm == Permissions.READ_WRITE) {
            if (tid.equals(pageExcludedByTransaction.get(pid))) return;
            acquireExclusiveLock(tid, pid);
        }
        updateTransactionLocks(tid, pid);
    }

    private void acquireShareLock(TransactionId tid, PageId pid) throws TransactionAbortedException {
        while (true) {
            synchronized (getLock(pid)) {
                ArrayList<TransactionId> holders = new ArrayList<>();
                TransactionId holder = pageExcludedByTransaction.get(pid);
                if (holder != null) holders.add(holder);
                holders.remove(tid);
                if (holders.isEmpty()) {
                    removeDependency(tid);
                    pageSharedByTransactions.putIfAbsent(pid, new ConcurrentLinkedDeque<>());
                    pageSharedByTransactions.get(pid).add(tid);
                    return;
                }
                updateDependency(tid, holders);
            }
        }
    }

    private void acquireExclusiveLock(TransactionId tid, PageId pid) throws TransactionAbortedException {
        while (true) {
            synchronized (getLock(pid)) {
                ArrayList<TransactionId> holders = new ArrayList<>();
                if (pageExcludedByTransaction.containsKey(pid)) holders.add(pageExcludedByTransaction.get(pid));
                if (pageSharedByTransactions.containsKey(pid)) holders.addAll(pageSharedByTransactions.get(pid));
                holders.remove(tid);
                if (holders.isEmpty()) {
                    removeDependency(tid);
                    pageExcludedByTransaction.put(pid, tid);
                    transactionHoldDirtyPages.putIfAbsent(tid, new ConcurrentLinkedDeque<>());
                    transactionHoldDirtyPages.get(tid).add(pid);
                    return;
                }
                updateDependency(tid, holders);
            }
        }
    }

    private void removeDependency(TransactionId tid) {
        synchronized (dependencyGraph) {
            dependencyGraph.remove(tid);
            for (TransactionId otherTid : dependencyGraph.keySet()) dependencyGraph.get(otherTid).remove(tid);
        }
    }

    private void updateDependency(TransactionId tid, Collection<TransactionId> holders) throws TransactionAbortedException {
        dependencyGraph.putIfAbsent(tid, new ConcurrentLinkedDeque<>());
        boolean change = false;
        for (TransactionId holder: holders)
            if (!dependencyGraph.get(tid).contains(holder)) {
                change = true;
                dependencyGraph.get(tid).add(holder);
            }
        if (change) checkDeadLock(tid, new HashSet<>());
    }

    private void checkDeadLock(TransactionId tid, HashSet<TransactionId> visit) throws TransactionAbortedException {
        if (!dependencyGraph.containsKey(tid)) return;
        for (TransactionId holder: dependencyGraph.get(tid)) {
            if (visit.contains(holder)) throw new TransactionAbortedException();
            visit.add(holder);
            checkDeadLock(holder, visit);
            visit.remove(holder);
        }
    }

    private void updateTransactionLocks(TransactionId tid, PageId pid) {
        transactionHoldPages.putIfAbsent(tid, new ConcurrentLinkedDeque<>());
        transactionHoldPages.get(tid).add(pid);
    }

    public void releasePage(TransactionId tid, PageId pid) {
        synchronized (getLock(pid)) {
            if (pageSharedByTransactions.containsKey(pid)) {
                pageSharedByTransactions.get(pid).remove(tid);
            }
            if (pageExcludedByTransaction.containsKey(pid) && pageExcludedByTransaction.get(pid).equals(tid)) {
                pageExcludedByTransaction.remove(pid);
            }
            if (transactionHoldPages.containsKey(tid)) {
                transactionHoldPages.get(tid).remove(pid);
            }
            if (transactionHoldDirtyPages.containsKey(tid)) {
                transactionHoldDirtyPages.get(tid).remove(pid);
            }
        }
    }

    public void releasePages(TransactionId tid) {
        if (!transactionHoldPages.containsKey(tid)) return;
        for (PageId pid : transactionHoldPages.get(tid)) releasePage(tid, pid);
        transactionHoldDirtyPages.remove(tid);
    }

    public Map<TransactionId, Collection<PageId>> getPagesDirtiedByTransaction() {
        return transactionHoldDirtyPages;
    }
}